# 캐시에서 발생할 수 있는 문제를 알아보자

<img width="9100" height="4556" alt="cache_stampede" src="https://github.com/user-attachments/assets/f563e35b-857a-481a-b77a-9485ae84a3f5" />


## 프로젝트 소개

캐시 사용 시 발생할 수 있는 **캐시 스탬피드(Cache Stampede)** 와 **핫키(Hot Key)** 문제를 시연하고, 이에 대한 해결책을 비교 테스트하기 위한 프로젝트입니다.  

다양한 캐시 조회 전략을 기준으로 API로 구현하였고 이를 `Junit` `k6`를 이용한 부하 테스트와 `Prometheus`, `Grafana`를 통한 실시간 모니터링 환경을 통해 각 전략의 성능과 효과를 직관적으로 확인할 수 있도록 구성하였습니다.  

[Redis에게 캐시 잘 먹이는 방법 – 캐시 스탬피드와 핫키](https://an-jiohh.github.io/blog/Backendcache_stampede)

해당 링크에서 정리된 글을 확인할 수 있습니다.

## 비교할 해결 전략

캐시 스탬피드와 핫키 문제에 대응하기 위한 세 가지 전략을 구현하여 비교하였습니다.  

1.  **기본 캐싱 (`@Cacheable`)**
    *   `GET /item/{id}`
    *   Spring의 `@Cacheable` 애노테이션을 사용한 가장 기본적인 캐싱 방식
    *   캐시 스탬피드 문제에 취약한 구조

2.  **Jitter를 이용한 만료 시간 분산**
    *   `GET /item/jitter/{id}`
    *   캐시의 TTL(Time-To-Live)에 랜덤한 시간(Jitter)을 추가하여 캐시 만료 시점을 분산시키는 전략
    *   여러 서버의 캐시가 동시에 만료되는 것을 방지하여 캐시 스탬피드 현상을 완화

3.  **분산 락 (Distributed Lock)을 이용한 DB 접근 제어**
    *   `GET /item/hotSafe/{id}`
    *   Redisson의 분산 락을 사용하여 캐시 미스 발생 시, 오직 하나의 스레드(또는 프로세스)만이 데이터베이스에 접근하여 캐시를 갱신하도록 제어
    *   다른 요청들은 락이 해제될 때까지 대기하다가, 갱신된 캐시 데이터를 사용

## API 엔드포인트

- `GET /item/{id}`: 기본 캐싱 전략으로 아이템을 조회합니다.
- `GET /item/jitter/{id}`: Jitter를 이용한 전략으로 아이템을 조회합니다.
- `GET /item/hotSafe/{id}`: 분산 락을 이용한 전략으로 아이템을 조회합니다.
- `GET /stats`: 현재까지 발생한 캐시 미스 횟수를 확인합니다.
- `GET /reset`: 캐시 미스 카운터를 초기화합니다.

## 기술 스택

- **Backend**: `Java 21`, `Spring Boot 3`
- **Database**: `MySQL`
- **Cache**: `Redis`
- **Distributed Lock**: `Redisson`
- **Build Tool**: `Gradle`
- **Containerization**: `Docker`, `Docker Compose`
- **Load Testing**: `k6`
- **Monitoring**: `Prometheus`, `Grafana`

## 시작하기

1.  프로젝트 클론
    ```bash
    git clone https://github.com/your-username/cache-stampede.git
    cd cache-stampede
    ```

2.  Docker Compose 실행
    프로젝트 루트 디렉토리에서 다음 명령어를 실행하여 모든 서비스(Application, MySQL, Redis, Prometheus, Grafana)를 시작합니다.
    ```bash
    docker-compose up -d
    ```

3.  서비스 확인
    - Application: `http://localhost:8080`
    - Grafana: `http://localhost:3000`
    - Prometheus: `http://localhost:9090`

4. 테스트 진행
    - junit을 이용한 테스트 진행
    - K6를 이용한 테스트 진행  
      `k6` 디렉토리 내부 테스트 스크립트 활용
