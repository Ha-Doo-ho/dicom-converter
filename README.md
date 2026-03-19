# 🏥DICOM to PNG Converter Web Service

# 소개
병원 등에서 사용되는 의료 영상 표준 파일 (DICOM, .dcm)을 웹 브라우저에서 쉽게 확인할 수 있도록 PNG이미지로 자동 변환하고 메타데이터를 관리하는 벡엔드 서버입니다.

# 사용한 엔진
1. JAVA엔진 : PlanA로 'IageIO'를 사용하여 메모리 효율적이고 빠른 1차 이미지 변환을 시도합니다.
2. Python엔진 : JAVA에서 처리하기 어려운 특수 포맷의 DICOM 파일 등에서 예외가 발생하면, 프로세스가 종료되지 않고 ProcessBuilde를 통해 외부 Python 스크립트로 2차 변환을 수행합니다.

# 기술 스택
**Backend Framework:** Java 17+, Spring Boot
**Database / ORM: ** MySQL, Spring Data JPA
**Python Scripting:** Python 3.x
**Medical Imaging:** dcm4che3 (Java)

## 주요 API 명세
|HTTP Method | Endpoint | 설명 |
|---|---|---|
| `POST` | `/api/dicom/upload` | DICOM 파일 업로드 및 메타데이터 추출/저장 |
| `POST` | `/api/dicom/convert/{id}` | 업로드된 DICOM 파일을 PNG로 변환 |
| `GET` | `/api/dicom/history` | 전체 파일 업로드 및 변환 이력 조회 |
| `GET` | `/api/dicom/history/{id}` | 특정 파일의 상세 메타데이터 조회 |
| `GET` | `/api/dicom/download/{id}` | 변환 완료된 PNG 파일 다운로드 (헤더 적용) |
