#!/usr/bin/env bash
# EDA quick check script
# - Health 확인
# - 정상 등록 → outbox publish 성공 & backlog 0 확인(폴링)
# - (옵션) 격리/Quarantine 검증
# - (옵션) DLT 검증

APP_BASE=${APP_BASE:-http://localhost:6060}
HEALTH_URL="$APP_BASE/actuator/health"
METRICS_URL="$APP_BASE/actuator/prometheus"
POST_URL="$APP_BASE/member"

# 옵션 플래그 (원하면 1로 설정)
RUN_QUARANTINE=${RUN_QUARANTINE:-0}
RUN_DLT=${RUN_DLT:-0}

# ========== helpers ==========
say() { echo -e "$@"; }
hr() { printf '%*s\n' "${COLUMNS:-80}" '' | tr ' ' '='; }

# 메트릭 한 개 값 추출 (없으면 0)
get_metric() {
  local name="$1"
  curl -fsS "$METRICS_URL" \
    | awk -v n="$name" '
      $1==n && $2 ~ /^[0-9.]+$/ {print $2; found=1}
      END{if(!found) print "0"}'
}

# 0(0.0 포함) 판정
is_zero() { awk -v x="$1" 'BEGIN{exit ((x+0)==0)?0:1}'; }

# 부동소수 비교: a > b
is_gt() { awk -v a="$1" -v b="$2" 'BEGIN{exit (a>b)?0:1}'; }

# 고유 이메일 생성
unique_email() { date +%s | awk '{print "test"$1"@example.com"}'; }

# ========== 1) Health ==========
hr
say "== 1) /actuator/health =="
if curl -fsS "$HEALTH_URL" | grep -q '"status":"UP"'; then
  say "[OK] 앱 UP"
else
  say "[FAIL] 앱이 UP 이 아님"
  exit 1
fi

# ========== 기준 스냅샷 ==========
hr
say "== 기준 메트릭 스냅샷 =="
S0=$(get_metric outbox_publish_success_total)
F0=$(get_metric outbox_publish_failure_total)
B0=$(get_metric outbox_backlog)
Q0=$(get_metric outbox_quarantine_total)
CS0=$(get_metric member_consume_success_total)
CF0=$(get_metric member_consume_failure_total)
CD0=$(get_metric member_consume_dlt_total)
say "  publish_success=$S0, publish_failure=$F0, backlog=$B0, quarantine=$Q0"
say "  consume_success=$CS0, consume_failure=$CF0, consume_dlt=$CD0"

# ========== 2) 정상 등록 → publish 성공 & backlog 0 ==========
hr
say "== 2) 정상 등록 → Outbox 발행 성공 & backlog 0 =="
EMAIL="$(unique_email)"
RESP=$(curl -fsS -X POST "$POST_URL" \
  -H 'Content-Type: application/json' \
  -d '{"email":"'"$EMAIL"'","name":"Alice"}')
say "  response: $RESP"

# 폴링(최대 15초): success > S0 && backlog == 0
deadline=$((SECONDS+15))
while :; do
  S=$(get_metric outbox_publish_success_total)
  B=$(get_metric outbox_backlog)
  say "  publish_success(now)=$S, backlog(now)=$B"
  if is_gt "$S" "$S0" && is_zero "$B"; then
    say "[OK] 발행 성공 카운터 증가 & backlog 0"
    break
  fi
  (( SECONDS >= deadline )) && { say "[FAIL] 타임아웃 (success:$S, backlog:$B)"; exit 1; }
  sleep 1
done

# ========== 3) (옵션) 격리/Quarantine 테스트 ==========
if [[ "$RUN_QUARANTINE" == "1" ]]; then
  hr
  say "== 3) Quarantine 테스트 (필드 누락) =="
  BAD_EMAIL="$(unique_email)"
  RESP=$(curl -fsS -X POST "$POST_URL" \
    -H 'Content-Type: application/json' \
    -d '{"email":"'"$BAD_EMAIL"'","name":""}')
  say "  response(bad): $RESP"

  # 폴링(최대 15초): quarantine_total > Q0
  deadline=$((SECONDS+15))
  while :; do
    Q=$(get_metric outbox_quarantine_total)
    say "  quarantine(now)=$Q"
    if is_gt "$Q" "$Q0"; then
      say "[OK] quarantine_total 증가"
      break
    fi
    (( SECONDS >= deadline )) && { say "[FAIL] 타임아웃 (quarantine:$Q)"; exit 1; }
    sleep 1
  done
fi

# ========== 4) (옵션) DLT 테스트 ==========
if [[ "$RUN_DLT" == "1" ]]; then
  hr
  say "== 4) DLT 테스트 (컨슈머에서 강제 예외) =="
  DLT_EMAIL="$(unique_email)"
  RESP=$(curl -fsS -X POST "$POST_URL" \
    -H 'Content-Type: application/json' \
    -d '{"email":"'"$DLT_EMAIL"'","name":"DLT"}')
  say "  response(dlt): $RESP"

  # 폴링(최대 30초): member_consume_dlt_total > CD0
  deadline=$((SECONDS+30))
  while :; do
    CD=$(get_metric member_consume_dlt_total)
    CF=$(get_metric member_consume_failure_total)
    say "  consume_dlt(now)=$CD, consume_failure(now)=$CF"
    if is_gt "$CD" "$CD0"; then
      say "[OK] DLT 카운터 증가"
      break
    fi
    (( SECONDS >= deadline )) && { say "[FAIL] 타임아웃 (dlt:$CD)"; exit 1; }
    sleep 1
  done
fi

hr
say "모든 체크 완료 ✅"
