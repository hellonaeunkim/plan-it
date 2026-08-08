# AI 챗봇 문서

AI 챗봇의 프롬프트·컨텍스트 설계와 단계별 평가 근거를 보관한다.

## 문서 구조

```text
ai-chatbot/
├── design/       구현 전 설계와 결정
└── evaluation/
    ├── guide.md  평가 실행 및 채점 기준
    ├── reports/  사람이 검토한 단계별 결과
    └── artifacts/ 평가 실행기가 생성한 JSON 원본
```

## 읽는 순서

1. [프롬프트·컨텍스트·응답 설계](./design/prompt-context-response-design.md)
2. [토큰·답변 품질 평가 가이드](./evaluation/guide.md)
3. 단계별 평가 결과
   - [기준선](./evaluation/reports/baseline-3c0c931.md)
   - [정책과 사용자 컨텍스트 경계](./evaluation/reports/prompt-boundary-e66b0dd.md)
   - [응답 근거 제한 v3](./evaluation/reports/response-policy-539d293.md)
   - [응답 근거 제한 v4](./evaluation/reports/response-policy-refined-dc84c46.md)
   - [응답 근거 제한 v5](./evaluation/reports/response-policy-v5-95f7fa4.md)
   - [일정 조회 범위 제한](./evaluation/reports/schedule-scope-fc6916b.md)
   - [최근 3턴 대화 윈도우](./evaluation/reports/recent-window-62779fd.md)

`reports`의 수치와 해석은 같은 파일명을 가진 `artifacts`의 JSON을 근거로 작성한다.
