# CLAUDE.md

이 문서는 Claude Code가 이 저장소에서 작업할 때 따라야 할 핵심 규칙만 담는다.
상세 배경/팀 규칙은 별도 문서를 참조한다.

## 참조 문서

- Git/브랜치 컨벤션 전문: [`docs/git-convention.md`](docs/git-convention.md)

## 작업 시 반드시 지킬 것

- `main` 브랜치에 **직접 push 금지**. 모든 작업은 `dev`에서 분기한 issue 브랜치에서 진행한다.
- 작업 전, 현재 브랜치가 `dev`에서 분기된 issue 브랜치인지 확인한다.
- 병합 방향은 `main` ← `dev` ← `issue branch`. PR을 통해서만 merge한다.
- 네이밍은 모두 **영문 소문자**, `작업내용요약`은 **동사-목적어** 형태 2~4단어.
  - 브랜치: `[task]/[issue-no]-[summary]` (예: `feature/12-develop-bert4rec`)
  - 커밋: `[task]: [summary] (#[issue-no])` (예: `data: preprocess news crawling data (#7)`)
  - PR 제목: 커밋 메시지와 동일
- 커밋 메시지 끝에 반드시 `(#[issue-no])`를 포함한다.
- Task 유형: `data` / `feature` / `bug` / `refactor` / `config` / `chores`
- 다음 파일은 절대 커밋/푸시하지 않는다.
  - `.env`, 토큰/API 키, WandB 키 등 민감 파일
  - 실험 산출물(log, submits 등), hyperparameter 튜닝 결과표