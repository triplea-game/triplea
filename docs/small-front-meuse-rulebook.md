# Small Front: Meuse Corridor 룰북

이 문서는 `Small Front: Meuse Corridor` 시나리오의 플레이어용 규칙을 정리한다. 규칙 충돌이 있을 경우 현재 시나리오 XML과 엔진 구현이 최종 기준이다.

## 1. 시나리오 개요

- 플레이어: **Germans**, **Americans**
- 기본 종료 시점: **8라운드 종료 시 작전 점수 계산**
- 경제·구매 단계 없음
- 각 진영은 정해진 일정에 따라 고정 증원을 받음
- 핵심 시스템:
  - 도로 기반 보급
  - 지형별 스택 용량
  - 전투 이동과 재배치 이동 분리
  - 지상전과 공중전 분리
  - 공중 우세에 따른 지상 공격 보너스
  - 전장의 안개
  - 작전 목표 점수

## 2. 승리 조건과 점수

`Auto Termination`이 켜져 있으면 8라운드 종료 시 게임이 끝나고 점수가 높은 진영이 승리한다. 옵션을 끄면 8라운드 이후에도 계속 플레이할 수 있다.

### 2.1 공통 목표 점수

다음 승리 도시를 소유할 때마다 **1점**을 얻는다.

- Bastogne
- Dinant
- Marche
- Namur
- Neufchateau
- St. Vith

### 2.2 독일군 작전 보너스

독일군은 다음 조건으로 추가 점수를 얻는다.

- **+2점:** Huy, Andenne, Namur, Dinant, Givet 중 적어도 한 곳에 **보급된 독일 지상군**이 존재
- **+1점:** Bastogne, Marche 중 적어도 한 곳에 **보급된 독일 지상군**이 존재

단순 점령만으로는 이 보너스를 받지 않는다. 해당 지역에 보급 상태인 독일 지상군이 실제로 있어야 한다.

### 2.3 미군 작전 보너스

- **+2점:** Huy, Andenne, Namur, Dinant, Givet 어디에도 **보급된 독일 지상군**이 존재하지 않음

### 2.4 동점

점수가 같으면 **Americans가 승리**한다.

## 3. 라운드와 턴 순서

한 라운드는 다음 순서로 진행된다.

### 독일군 턴

1. 고정 증원
2. 보급 판정
3. 전투 이동
4. 전투
5. 재배치
6. 턴 종료

### 미군 턴

1. 고정 증원
2. 보급 판정
3. 전투 이동
4. 전투
5. 재배치
6. 턴 종료

그 뒤 라운드 종료 처리를 수행한다.

## 4. 부대 능력치

| 부대 | 공격 | 방어 | 전투 이동 | 재배치 이동 | 스택 비용 | 특수 규칙 |
|---|---:|---:|---:|---:|---:|---|
| infantry | 1 | 2 | 1 | 1 | 1 | 독일군 보병, 포병 지원 가능 |
| americanInfantry | 1 | 2 | 1 | 2 | 1 | 미군 보병, 전 군 차량화로 재배치 이동 +1, 포병 지원 가능 |
| artillery | 2 | 2 | 1 | 1 | 1 | 모든 지상 전투 부대 지원 |
| selfPropelledArtillery | 2 | 2 | 2 | 3 | 1 | 돌격포, 포병 화력 + 기계화 이동, 모든 지상 전투 부대 지원 |
| armour | 2 | 3 | 2 | 3 | 2 | 전격 가능, 포병 지원 가능 |
| mechanized | 1 | 2 | 2 | 3 | 1 | 기계화 보병, 포병 지원 가능, 기갑 1개 공격 +1 지원 |
| fighter | 3 | 3 | 4 | 4 | 0 | 공중전·요격·호위 가능 |

전투 판정은 6면체 주사위를 사용한다. 일반 TripleA 전투 규칙에 따라 각 부대는 자신의 공격 또는 방어 수치 이하를 굴리면 명중한다.

- **americanInfantry**는 미군 전용 보병이다. 능력치는 독일군 보병과 같지만 미군이 전 군 차량화되었음을 반영해 재배치(비전투) 이동이 1 더 높다.
- **selfPropelledArtillery**(돌격포)는 포병의 화력과 기계화 부대의 이동력을 함께 가진다. 독일군 포병 일부가 돌격포로 편성되어 있다.

### 포병 지원

일반 TripleA 포병 지원 규칙이 적용된다. 지원 부대(artillery, selfPropelledArtillery)는 함께 공격하는 지원 가능한 부대의 공격력을 향상시킨다. 이 시나리오에서는 보병뿐 아니라 armour와 mechanized를 포함한 **모든 지상 전투 부대**가 포병 지원을 받을 수 있다.

### 기계화-기갑 협동

공격 시 **mechanized 1개는 함께 전투하는 armour 1개의 공격력을 +1** 지원한다.

- 각 mechanized는 armour 1개만 지원할 수 있다.
- 각 armour는 이 규칙으로 최대 +1만 받을 수 있다.
- 방어 전투에는 적용되지 않는다.
- 포병 지원 및 공중 통제 보너스와는 별개의 보너스이므로 조건을 모두 충족하면 중첩된다.
- 이 지원은 독일군 mechanized에만 적용된다.


## 5. 지형

| 지형 | 지상전 최대 라운드 | 공중전 최대 라운드 | 스택 용량 |
|---|---:|---:|---:|
| Open | 4 | 2 | **7** |
| Town | 3 | 1 | **6** |
| Forest | 2 | 1 | **5** |

지형은 전투가 지속될 수 있는 최대 라운드와 해당 지역에 집결할 수 있는 지상 전력의 양을 결정한다.

## 6. 스택 용량

### 6.1 계산법

한 지역에 있는 아군 및 동맹군의 스택 비용 합계가 그 지형의 스택 용량을 넘을 수 없다.

- infantry: 1
- americanInfantry: 1
- artillery: 1
- selfPropelledArtillery: 1
- armour: 2
- mechanized: 1
- fighter: 0

예시:

- Forest 용량 5: `armour 1 + mechanized 1 + infantry 2` = 5
- Town 용량 6: `armour 2 + mechanized 1 + infantry 1` = 6
- Open 용량 7: `armour 2 + infantry 3` = 7

### 6.2 목적지에서만 검사

이동 중 통과하는 지역의 스택 용량은 검사하지 않는다. **최종 목적지**가 이동 부대 전체를 수용할 수 있어야 한다.

### 6.3 기존 병력과 대기 병력

- 목적지에 이미 있는 아군·동맹군이 용량을 사용한다.
- 같은 배치 또는 증원 처리에서 먼저 들어갈 예정인 병력도 용량을 사용한다.
- fighter처럼 스택 비용이 0인 부대는 용량이 가득 차도 진입할 수 있다.
- 이미 초과된 스택에는 스택 비용이 있는 부대를 추가할 수 없다.

### 6.4 적용 범위

스택 용량은 다음 경로에 공통 적용된다.

- 이동
- 배치
- 고정 증원
- 퇴각 및 기타 부대 진입

## 7. 보급

### 7.1 보급망 형성

보급은 **보급원**에서 시작해 도로로 연결된 우호 지역을 따라 전달된다.

보급 경로의 조건:

1. 출발점이 현재 자기 진영이 소유한 보급원이어야 한다.
2. 지도에 지정된 보급 도로를 따라야 한다.
3. 경로상의 지역은 자기 진영 또는 동맹 진영이 소유한 육지여야 한다.

보급 도로는 양방향으로 취급된다.

### 7.1.1 남부 우회 보급축

`Neufchateau–Libramont–Martelange–Bastogne` 도로망은 남부 미군의 우회 보급축을 형성한다. 따라서 **Wiltz 한 곳만 상실해도 Bastogne, Martelange, Saint-Hubert, Libramont가 즉시 고립되지는 않는다.** 독일군이 이 축을 차단하려면 Neufchateau, Libramont, Martelange 등 남부 전선의 실제 연결 지역을 추가로 점령해야 한다.

### 7.2 보급원

지도에 지정된 보급원은 다음과 같다.

- Andenne
- Bitburg
- Blankenheim
- Dinant
- Echternach
- Givet
- Huy
- Namur
- Prum

보급원은 진영 고정이 아니다. 해당 지역을 현재 소유한 진영이 사용할 수 있다.

### 7.3 보급 차단 효과

- 보급이 끊긴 **지상군은 이동할 수 없다.**
- 공군은 보급을 요구하지 않는다.
- 보급 판정은 각 진영의 전투 이동 전에 수행된다.
- 다시 보급되면 고립 누적이 즉시 초기화된다.

### 7.4 고립 부대 제거

지상군이 **자기 진영의 보급 단계 2회 연속** 보급되지 않으면 제거된다.

- 첫 번째 보급 실패: 고립 1턴
- 두 번째 보급 실패: 부대 제거

## 8. 이동

### 8.1 전투 이동

전투 이동에서는 적 지역으로 진입해 전투를 만들 수 있다.

- infantry, americanInfantry, artillery: 1
- armour, mechanized, selfPropelledArtillery: 2
- fighter: 4
- armour는 조건을 충족하면 전격 이동 가능
- 보급이 끊긴 지상군은 이동 불가
- 최종 목적지 스택 용량을 초과하는 이동은 불가

### 8.2 재배치

전투 후 생존 병력을 정비하고 후방·예비대를 재배치한다.

- infantry, artillery: 1
- americanInfantry: 2
- armour, mechanized, selfPropelledArtillery: 3
- fighter: 4

전투 이동에서 움직인 비공군 부대는 같은 턴의 재배치 단계에서 다시 이동할 수 없다. 공군은 전투 이동에서 사용하고 남은 이동력을 재배치에 사용할 수 있다.

## 9. 전투

### 9.1 지상전

적대 지상군이 같은 지역에 있으면 지상전이 발생한다. 전투는 해당 지형의 최대 지상전 라운드까지 진행된다.

- Open: 최대 4라운드
- Town: 최대 3라운드
- Forest: 최대 2라운드

최대 라운드 내에 한쪽이 제거되지 않으면 생존 부대와 엔진의 후속 전투 규칙에 따라 전투가 정리된다.

### 9.2 공중전 분리

공중전과 지상전은 분리되어 처리된다. 공중전이 있는 지역에서는 공중전 결과가 먼저 정리되고, 살아남은 항공 전력에 따라 공중 통제 상태가 결정된다.

- Open: 최대 공중전 2라운드
- Town, Forest: 최대 공중전 1라운드

### 9.3 공중 통제

- 공격측 항공기만 남으면 공격측이 공중 통제
- 방어측 항공기만 남으면 방어측이 공중 통제
- 양측 항공기가 모두 남으면 공역은 경합 상태
- 공중 통제는 지상 소유권과 별개의 상태
- 이 시나리오의 공중 통제는 영구적이지 않으며 현재 라운드에만 유효

지상 공격자가 해당 지역의 공중 통제를 보유하면 공격하는 **지상군의 공격 수치에 +1**을 받는다. 방어 수치와 공군·해군 전투 수치는 변하지 않는다.

## 10. 전장의 안개

전장의 안개가 활성화되어 있으며 시야 반경은 1이다.

시야는 다음 위치에서 시작한다.

- 자기 진영 또는 동맹 진영이 소유한 모든 지역
- 자기 진영 또는 동맹 진영 부대가 있는 모든 지역

각 시야 원천에서 일반 지도 연결을 따라 1칸까지 볼 수 있다.

숨겨진 지역에서도 지도 형태와 연결 관계는 공개되지만 다음 정보는 가려진다.

- 소유자
- 부대 구성
- 보급 상태
- 전투 및 동적 표식

이전 턴에 보았던 적 위치를 자동으로 기억해 주는 `last known position` 시스템은 없다.

## 11. 고정 증원

증원은 지정된 라운드의 증원 단계에 자동 도착한다.

- 목적지는 자기 진영 또는 동맹 진영 소유여야 한다.
- 스택 용량이 부족하면 가능한 수량만 배치된다.
- 들어가지 못한 나머지는 대기열에 남고 다음 증원 단계에 다시 시도된다.
- 점령당한 증원 목적지도 우호 지역이 될 때까지 대기한다.

### 11.1 독일군 증원 일정

| 라운드 | 목적지 | 부대 |
|---:|---|---|
| 1 | Prum | armour 2 |
| 1 | Bitburg | mechanized 1 |
| 2 | Prum | selfPropelledArtillery 1 |
| 2 | Blankenheim | armour 1 |
| 3 | Bitburg | infantry 2 |
| 3 | Prum | mechanized 1 |
| 4 | Echternach | infantry 2 |
| 4 | Bitburg | fighter 1 |
| 5 | Prum | infantry 2 |

### 11.2 미군 증원 일정

| 라운드 | 목적지 | 부대 |
|---:|---|---|
| 2 | Marche | americanInfantry 2 |
| 3 | Namur | armour 1 |
| 3 | Ciney | fighter 1 |
| 4 | Namur | americanInfantry 2 |
| 4 | Dinant | artillery 1 |
| 5 | Namur | armour 2 |
| 5 | Namur | fighter 1 |
| 6 | Huy | americanInfantry 2 |
| 6 | Givet | americanInfantry 2 |
| 7 | Namur | armour 1 |

## 12. 초기 배치

### 12.1 독일군

| 지역 | 초기 부대 |
|---|---|
| Prum | infantry 2, artillery 1, fighter 1 |
| Blankenheim | infantry 2, armour 1 |
| Bitburg | infantry 2, mechanized 1, fighter 1 |
| Echternach | infantry 2 |
| Losheim Gap | infantry 2, mechanized 1 |
| Clervaux | infantry 2, armour 1 |
| Vianden | infantry 2, selfPropelledArtillery 1 |

### 12.2 미군

| 지역 | 초기 부대 |
|---|---|
| St. Vith | americanInfantry 2, artillery 1 |
| Houffalize | americanInfantry 1 |
| Bastogne | americanInfantry 2, artillery 1 |
| Martelange | americanInfantry 1 |
| Vielsalm | americanInfantry 1 |
| La Roche | americanInfantry 1 |
| Erezee | americanInfantry 1 |
| Hotton | americanInfantry 1 |
| Nassogne | americanInfantry 1 |
| Libramont | americanInfantry 1 |
| Marche | americanInfantry 2, armour 1 |
| Neufchateau | americanInfantry 1 |
| Saint-Hubert | americanInfantry 1 |
| Ciney | americanInfantry 1, fighter 1 |
| Namur | americanInfantry 2, fighter 1 |
| Dinant | americanInfantry 1 |

초기 미군 전선 방어 병력은 이전 버전 대비 하향되었다. St. Vith와 Bastogne의 보병이 각각 1씩 줄었고, Houffalize는 1로 감소, 전방 돌출부인 Wiltz의 초기 병력은 제거되었다.

## 13. 전략 요약

### 독일군

- 8라운드 안에 전선을 서쪽으로 밀어야 한다.
- 단순 점령보다 **보급된 지상군으로 작전 보너스 지역을 유지**하는 것이 중요하다.
- Forest 전선의 용량은 5이므로 기갑과 보병을 함께 집중할 수 있지만 무제한 집결은 불가능하다.
- 공격 축을 너무 많이 만들면 보급망과 증원 거점이 약해진다.

### 미군

- 승리 도시를 지키고 독일군의 서부 작전 보너스를 차단해야 한다.
- 동점 승리 규칙 때문에 무리한 반격보다 핵심 지역 지연전이 유리할 수 있다.
- 독일군이 Huy–Givet 보너스 권역에 보급된 지상군을 남기지 못하게 하는 것이 중요하다.
- 후반 증원은 Namur와 서부 보급원 방어에 집중된다.

## 14. 빠른 참조

| 항목 | 값 |
|---|---|
| 점수 계산 | 8라운드 종료 |
| 동점 승자 | Americans |
| 보급 단절 제거 | 자기 보급 단계 2회 |
| 시야 반경 | 1 |
| Open 스택 | 7 |
| Town 스택 | 6 |
| Forest 스택 | 5 |
| 지상전 라운드 | Open 4 / Town 3 / Forest 2 |
| 공중전 라운드 | Open 2 / Town 1 / Forest 1 |
| 공중 통제 지상 공격 보너스 | +1 |
