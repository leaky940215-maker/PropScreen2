# PropScreen (촬영 현장용 HTML 풀스크린 플레이어)

## 1. GitHub에서 APK 자동 빌드하는 법 (PC 설치 불필요)
1. GitHub(github.com) 로그인 후 우측 상단 `+` -> `New repository` 클릭.
2. Repository name에 `PropScreen` 입력 후 `Create repository` 클릭 (Private / Public 상관없음).
3. 생성된 페이지에서 `uploading an existing file` 클릭.
4. 이 압축 파일(`PropScreen-Android-Project.zip`)의 **압축을 푼 내부 폴더/파일 전체**(`.github`, `app`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`)를 한꺼번에 드래그 앤 드롭 -> 하단 `Commit changes` 클릭.
5. 상단 탭에서 **Actions** 클릭 -> `Build PropScreen APK` 실행 완료 대기 (약 2~3분, 초록색 체크 표시).
6. 완료된 워크플로우 클릭 -> 하단 **Artifacts** 영역의 `PropScreen-APK` 클릭하여 다운로드.
7. 다운로드된 압축 파일 안의 `app-debug.apk`를 갤럭시 소품 폰으로 옮겨 설치.

## 2. 현장 사용법
- 앱 실행 후 `+ HTML 추가` 버튼으로 제작한 `.html` 소품 파일을 추가 (오프라인에서도 계속 유지됨).
- 목록에서 소품을 누르면 갤럭시 상단바/펀치홀/하단바까지 100% 풀스크린으로 실행.
- 촬영 중 나가기 또는 NG 리셋: **볼륨 하(-) 버튼 1.5초 꾹 누르기** (또는 좌측 상단 모서리 5회 연타).
