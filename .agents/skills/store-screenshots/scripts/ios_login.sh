#!/bin/bash
# usage: ios_login.sh UDID NAME [SIM_DISPLAY]  -- logs out (current locale = ko) then captures login in all locales
U=$1; N=$2; export SIM_DISPLAY=$3; P=com.kevlina.budgetplus.stage
L(){ "$(dirname "$0")/br.py" -- xcrun simctl launch --terminate-running-process $U $P -AppleLanguages "($1)" -AppleLocale $2 >/dev/null; sleep 5; }
L ko-KR ko_KR
"$(dirname "$0")/m.sh" $U <<'EOS'
- tapOn: "설정"
- scrollUntilVisible:
    element:
      text: "로그아웃"
- tapOn: "로그아웃"
- waitForAnimationToEnd
EOS
for t in "zh-Hant:zh-Hant-TW:zh_TW" "en-US:en-US:en_US" "zh-Hans:zh-Hans-CN:zh_CN" "ja:ja-JP:ja_JP" "ko:ko-KR:ko_KR"; do
  IFS=: read loc lang lc <<< "$t"; L $lang $lc; "$(dirname "$0")/cap.sh" $U ios $N $loc login | tail -1
done
