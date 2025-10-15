./gradlew :module:domain:app:bootRun --args='--server.port=7070 --spring.profiles.active=db'
./gradlew :module:platform:auth:bootRun --args='--server.port=7071 --spring.profiles.active=db'
./gradlew :module:registry:schema-registry:bootRun --args='--server.port=7072 --spring.profiles.active=db'

cd ~/data_mesh/docker
# 1) 레지스트리(Postgres) 마이그레이션
docker compose run flyway-registry
# 2) 앱(MySQL) 마이그레이션
docker compose run flyway-app
cd ~/data_mesh/
