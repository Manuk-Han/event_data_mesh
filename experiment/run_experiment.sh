#!/usr/bin/env bash
# run_experiment.sh
# 세트: EDA(6060/6061/6062), PURE(7070/7071/7072)
# - 부하(정상/스키마오류)
# - 장애 주입(기본: HTTP pause/resume) / docker/k8s 옵션 지원
# - Prometheus 메트릭 스크랩(세트 합산) & 결과 CSV 생성
set -euo pipefail

############################
# 0) 세트/엔드포인트/파라미터
############################
EDA_SET=(6060 6061 6062)
PURE_SET=(7070 7071 7072)

EVENT_PATH="${EVENT_PATH:-/api/members}"
PROM_PATH="${PROM_PATH:-/actuator/prometheus}"
HEALTH_PATH="${HEALTH_PATH:-/actuator/health}"

# 장애 주입 전략: auto|docker|k8s|http|none  (기본: http)
FAULT_STRATEGY="${FAULT_STRATEGY:-http}"

# docker/k8s/http 각 수단의 타겟(환경에 맞게 바꾸거나 export로 주입)
EDA_CONSUMER_CTN="${EDA_CONSUMER_CTN:-}"     # 예: eda-consumer-6062 (컨테이너 쓴다면)
K8S_DEPLOY="${K8S_DEPLOY:-}"                 # 예: eda-consumer      (K8s 쓴다면)
HTTP_PAUSE_URL="${HTTP_PAUSE_URL:-http://localhost:6062/ops/consumer/pause}"
HTTP_RESUME_URL="${HTTP_RESUME_URL:-http://localhost:6062/ops/consumer/resume}"

# 이벤트 건수
N1="${N1:-1000}"   # 정상 부하
N2="${N2:-200}"    # 스키마 오류 부하
N3="${N3:-1000}"   # 장애 주입 중 부하

# 부하 전송 방식: first|rr (세트 내 첫 포트만/라운드로빈)
LOAD_MODE="${LOAD_MODE:-first}"

# 메트릭 필터(환경에 맞게 바꾸면 됨)
METRICS_REGEX="${METRICS_REGEX:-outbox_publish_success_total|outbox_publish_failure_total|outbox_backlog|quarantine_event_total|member_consume_success_total|member_consume_failure_total|member_consume_dlt_total|process_latency_ms_(sum|count|bucket)|process_cpu_usage|jvm_memory_used_bytes}"

# backlog 메트릭 이름(합산 계산에 사용) - 실제 노출명과 다르면 교체
BACKLOG_METRIC_NAME="${BACKLOG_METRIC_NAME:-outbox_backlog}"

OUT_DIR="./experiment_results_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$OUT_DIR"

###################################
# 1) 공통 유틸/체크/스크랩/집계 함수
###################################
log()  { printf "[%s] %s\n" "$(date +%H:%M:%S)" "$*"; }
warn() { printf "[%s] [WARN] %s\n" "$(date +%H:%M:%S)" "$*" | tee -a "${OUT_DIR}/WARNINGS.txt"; }

health_wait () {
  local port="$1"
  local base="http://localhost:${port}"
  printf "Waiting for %s " "$base"
  until curl -sf "${base}${HEALTH_PATH}" >/dev/null ; do
    printf "."
    sleep 1
  done
  echo " OK"
}

health_check_set () { local ports=("$@"); for p in "${ports[@]}"; do health_wait "$p"; done; }

scrape_one () {
  local tag="$1" ; local port="$2"
  local base="http://localhost:${port}"
  curl -s "${base}${PROM_PATH}" | grep -E "${METRICS_REGEX}" > "${OUT_DIR}/metrics_${tag}_${port}.prom" || true
}

scrape_set () {
  local tag="$1" ; shift
  local ports=("$@")
  local files=()
  for p in "${ports[@]}"; do
    scrape_one "$tag" "$p"
    files+=("${OUT_DIR}/metrics_${tag}_${p}.prom")
  done
  awk 'NF>=2 {a[$1]+=$2} END { for(k in a) print k" "a[k] }' "${files[@]}" > "${OUT_DIR}/metrics_${tag}_SUM.prom" || true
}

calc_delta () {
  local before="$1" ; local after="$2" ; local out="$3"
  echo "metric,delta" > "$out"
  awk 'FNR==NR{a[$1]=$2;next} {print $1"," ($2 - a[$1])}' \
    <(grep -E "^(outbox_publish_success_total|outbox_publish_failure_total|quarantine_event_total|member_consume_success_total|member_consume_failure_total|member_consume_dlt_total|'${BACKLOG_METRIC_NAME//\//\\/}')" "$before" | awk '{print $1" "$2}') \
    <(grep -E "^(outbox_publish_success_total|outbox_publish_failure_total|quarantine_event_total|member_consume_success_total|member_consume_failure_total|member_consume_dlt_total|'${BACKLOG_METRIC_NAME//\//\\/}')" "$after"  | awk '{print $1" "$2}') \
    >> "$out" || true
}

sum_backlog_for_set() {
  local ports=("$@")
  local sum="0"
  for p in "${ports[@]}"; do
    v=$(curl -s "http://localhost:${p}${PROM_PATH}" | grep -E "^${BACKLOG_METRIC_NAME//\//\\/} " | awk '{print $2}' | head -1)
    v=${v:-0}
    sum=$(awk -v a="$sum" -v b="$v" 'BEGIN{print a+b}')
  done
  echo "$sum"
}

backlog_zero_wait() {
  local ports=("$@")
  log "backlog 0 대기 (set: ${ports[*]})"
  for i in {1..180}; do
    SUM=$(sum_backlog_for_set "${ports[@]}")
    echo "backlog_sum=${SUM}"
    awk "BEGIN{exit !(${SUM}==0)}" && return 0
    sleep 2
  done
  return 1
}

#####################################
# 2) 부하(정상/스키마 오류) 전송 함수
#####################################
fire_normal_load_first () {
  local ports=("$@"); local tag="${ports[-1]}"; unset 'ports[-1]'
  local base="http://localhost:${ports[0]}"
  log ">>> [${tag}] 정상 이벤트 ${N1}건 전송 -> ${base}${EVENT_PATH}"
  for ((i=1;i<=N1;i++)); do
    RAND=$RANDOM
    curl -s -o /dev/null -w "%{http_code}\n" -X POST "${base}${EVENT_PATH}" \
      -H "Content-Type: application/json" \
      -d "{\"memberId\":\"m-${RAND}\",\"email\":\"user${RAND}@example.com\",\"name\":\"User ${RAND}\"}" \
      >> "${OUT_DIR}/http_${tag}.log"
  done
}

fire_normal_load_rr () {
  local ports=("$@"); local tag="${ports[-1]}"; unset 'ports[-1]'
  log ">>> [${tag}] 정상 이벤트 ${N1}건 RR -> ${ports[*]}${EVENT_PATH}"
  for ((i=1;i<=N1;i++)); do
    RAND=$RANDOM
    p=${ports[$(( (i-1) % ${#ports[@]} ))]}
    curl -s -o /dev/null -w "%{http_code}\n" -X POST "http://localhost:${p}${EVENT_PATH}" \
      -H "Content-Type: application/json" \
      -d "{\"memberId\":\"m-${RAND}\",\"email\":\"user${RAND}@example.com\",\"name\":\"User ${RAND}\"}" \
      >> "${OUT_DIR}/http_${tag}.log"
  done
}

fire_schema_violations_first () {
  local ports=("$@"); local tag="${ports[-1]}"; unset 'ports[-1]'
  local base="http://localhost:${ports[0]}"
  log ">>> [${tag}] 스키마 오류 이벤트 ${N2}건 전송 -> ${base}${EVENT_PATH}"
  for ((i=1;i<=N2;i++)); do
    RAND=$RANDOM
    curl -s -o /dev/null -w "%{http_code}\n" -X POST "${base}${EVENT_PATH}" \
      -H "Content-Type: application/json" \
      -d "{\"memberId\":\"m-${RAND}\",\"name\":\"User ${RAND}\"}" \
      >> "${OUT_DIR}/http_${tag}.log"
  done
}

fire_schema_violations_rr () {
  local ports=("$@"); local tag="${ports[-1]}"; unset 'ports[-1]'
  log ">>> [${tag}] 스키마 오류 이벤트 ${N2}건 RR -> ${ports[*]}${EVENT_PATH}"
  for ((i=1;i<=N2;i++)); do
    RAND=$RANDOM
    p=${ports[$(( (i-1) % ${#ports[@]} ))]}
    curl -s -o /dev/null -w "%{http_code}\n" -X POST "http://localhost:${p}${EVENT_PATH}" \
      -H "Content-Type: application/json" \
      -d "{\"memberId\":\"m-${RAND}\",\"name\":\"User ${RAND}\"}" \
      >> "${OUT_DIR}/http_${tag}.log"
  done
}

fire_normal_load_fault_window () {
  local base="http://localhost:${EDA_SET[0]}"
  log ">>> [eda_fault] 다운 중 부하(N3=${N3}) -> ${base}${EVENT_PATH}"
  for ((i=1;i<=N3;i++)); do
    RAND=$RANDOM
    curl -s -o /dev/null -w "%{http_code}\n" -X POST "${base}${EVENT_PATH}" \
      -H "Content-Type: application/json" \
      -d "{\"memberId\":\"m-${RAND}\",\"email\":\"user${RAND}@example.com\",\"name\":\"User ${RAND}\"}" \
      >> "${OUT_DIR}/http_eda_fault.log"
  done
}

####################################
# 3) 장애 주입 자동 탐지/수행 함수
####################################
has_docker_container() { docker ps --format '{{.Names}}' | grep -Fxq "$1"; }
has_k8s_deploy() { command -v kubectl >/dev/null 2>&1 && kubectl get deploy "$1" >/dev/null 2>&1; }

fault_stop() {
  case "$FAULT_STRATEGY" in
    docker|auto)
      if [[ -n "${EDA_CONSUMER_CTN:-}" ]] && has_docker_container "$EDA_CONSUMER_CTN"; then
        docker stop "$EDA_CONSUMER_CTN" && return 0
      fi
      [[ "$FAULT_STRATEGY" == "docker" ]] && return 1
      ;;&
    k8s|auto)
      if [[ -n "${K8S_DEPLOY:-}" ]] && has_k8s_deploy "$K8S_DEPLOY"; then
        kubectl scale deploy "$K8S_DEPLOY" --replicas=0 && return 0
      fi
      [[ "$FAULT_STRATEGY" == "k8s" ]] && return 1
      ;;&
    http|auto)
      if [[ -n "${HTTP_PAUSE_URL:-}" ]]; then
        curl -sf -X POST "${HTTP_PAUSE_URL}" && return 0
      fi
      [[ "$FAULT_STRATEGY" == "http" ]] && return 1
      ;;&
    none) return 1 ;;
  esac
  return 1
}

fault_start() {
  case "$FAULT_STRATEGY" in
    docker|auto)
      if [[ -n "${EDA_CONSUMER_CTN:-}" ]] && has_docker_container "$EDA_CONSUMER_CTN"; then
        docker start "$EDA_CONSUMER_CTN" && return 0
      fi
      [[ "$FAULT_STRATEGY" == "docker" ]] && return 1
      ;;&
    k8s|auto)
      if [[ -n "${K8S_DEPLOY:-}" ]] && has_k8s_deploy "$K8S_DEPLOY"; then
        kubectl scale deploy "$K8S_DEPLOY" --replicas=1 && return 0
      fi
      [[ "$FAULT_STRATEGY" == "k8s" ]] && return 1
      ;;&
    http|auto)
      if [[ -n "${HTTP_RESUME_URL:-}" ]]; then
        curl -sf -X POST "${HTTP_RESUME_URL}" && return 0
      fi
      [[ "$FAULT_STRATEGY" == "http" ]] && return 1
      ;;&
    none) return 1 ;;
  esac
  return 1
}

##########################
# 4) 드라이버: 실험 시나리오
##########################
log "====[ 세트 헬스 체크 ]===="
health_check_set "${PURE_SET[@]}"
health_check_set "${EDA_SET[@]}"

# A) PURE baseline
log "====[ A) PURE baseline (${PURE_SET[*]}) ]===="
scrape_set "pure_before" "${PURE_SET[@]}"
if [[ "$LOAD_MODE" == "rr" ]]; then
  fire_normal_load_rr "${PURE_SET[@]}" "pure_normal"
else
  fire_normal_load_first "${PURE_SET[@]}" "pure_normal"
fi
sleep 5
scrape_set "pure_after" "${PURE_SET[@]}"
calc_delta "${OUT_DIR}/metrics_pure_before_SUM.prom" "${OUT_DIR}/metrics_pure_after_SUM.prom" "${OUT_DIR}/delta_pure.csv"

# B) EDA proposed
log "====[ B) EDA proposed (${EDA_SET[*]}) ]===="

# B1) 정상 부하
scrape_set "eda_before_normal" "${EDA_SET[@]}"
if [[ "$LOAD_MODE" == "rr" ]]; then
  fire_normal_load_rr "${EDA_SET[@]}" "eda_normal"
else
  fire_normal_load_first "${EDA_SET[@]}" "eda_normal"
fi
sleep 5
scrape_set "eda_after_normal" "${EDA_SET[@]}"
calc_delta "${OUT_DIR}/metrics_eda_before_normal_SUM.prom" "${OUT_DIR}/metrics_eda_after_normal_SUM.prom" "${OUT_DIR}/delta_eda_normal.csv"

# B2) 스키마 오류 → quarantine 유도
scrape_set "eda_before_quarantine" "${EDA_SET[@]}"
if [[ "$LOAD_MODE" == "rr" ]]; then
  fire_schema_violations_rr "${EDA_SET[@]}" "eda_quarantine"
else
  fire_schema_violations_first "${EDA_SET[@]}" "eda_quarantine"
fi
sleep 5
scrape_set "eda_after_quarantine" "${EDA_SET[@]}"
calc_delta "${OUT_DIR}/metrics_eda_before_quarantine_SUM.prom" "${OUT_DIR}/metrics_eda_after_quarantine_SUM.prom" "${OUT_DIR}/delta_eda_quarantine.csv"

# B3) 장애 주입(컨슈머 STOP/PAUSE) → backlog↑ → 재가동/RESUME → 복구시간
log ">>> [eda_fault] 장애 주입 시도 (strategy=${FAULT_STRATEGY})"
if fault_stop; then
  date +%s > "${OUT_DIR}/eda_fault_start.ts"
else
  warn "장애 주입 수단을 찾지 못했습니다 (docker/k8s/http/none). B3 스킵."
fi

# 다운 중 부하
fire_normal_load_fault_window
sleep 3

log ">>> [eda_fault] 복구 시도"
if fault_start; then
  date +%s > "${OUT_DIR}/eda_fault_recover.ts"
else
  warn "복구 수단을 찾지 못했습니다. B3 복구 단계 스킵."
fi

# backlog 0 대기 & 스크랩
if backlog_zero_wait "${EDA_SET[@]}"; then
  date +%s > "${OUT_DIR}/eda_fault_backlog_zero.ts"
fi
scrape_set "eda_after_fault" "${EDA_SET[@]}"

# 복구 시간 계산
if [[ -f "${OUT_DIR}/eda_fault_start.ts" && -f "${OUT_DIR}/eda_fault_backlog_zero.ts" ]]; then
  START=$(cat "${OUT_DIR}/eda_fault_start.ts")
  ZERO=$(cat "${OUT_DIR}/eda_fault_backlog_zero.ts")
  echo "metric,value_sec" > "${OUT_DIR}/recovery_time_eda.csv"
  echo "recovery_time,$((ZERO - START))" >> "${OUT_DIR}/recovery_time_eda.csv"
fi

##################
# 5) 요약 인덱스
##################
log "====[ 요약 ]===="
{
  echo "file,description"
  echo "delta_pure.csv, PURE 정상부하 전후 메트릭 변화(7070/7071/7072 합산)"
  echo "delta_eda_normal.csv, EDA 정상부하 전후 메트릭 변화(6060/6061/6062 합산)"
  echo "delta_eda_quarantine.csv, EDA 스키마오류 전후 변화(합산)"
  echo "recovery_time_eda.csv, EDA 장애 주입 후 backlog 0까지 복구시간"
  [[ -f "${OUT_DIR}/WARNINGS.txt" ]] && echo "WARNINGS.txt, 경고 메시지(장애주입 수단 미탐지 등)"
} > "${OUT_DIR}/INDEX.csv"

echo "결과 폴더: ${OUT_DIR}"
ls -1 "${OUT_DIR}"
