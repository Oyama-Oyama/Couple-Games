# Round 14 · Phase 4 运行时稳定性与功能回归报告

- 执行人：稳稳（android-runtime）
- 设备：`emulator-5556`（900×1600，Android 12L / API 32）
- 构建：`app/build/outputs/apk/debug/app-debug.apk`（本轮 09:47 产物，versionCode=210）
- 抓包方式：`adb logcat -d -v threadtime` 全量导出至 `audit_report/logcat_round14.txt`
- UI 取证：`adb shell uiautomator dump /sdcard/ui.xml`（`MSYS_NO_PATHCONV=1`），原始帧保存在 `audit_report/ui_*.xml`
- 说明：本轮**不改任何 `app/src/**` 文件**，仅产出本报告。

---

## 0. 结论摘要（不粉饰）

| 项目 | 结果 |
|---|---|
| P0：18+ 年龄确认按钮失效 → 首启白屏 | **实锤，已上报并指派修复；本轮构建未修复，待修复后复验** |
| P0：今晚一句话两个保存入口均无效 | **实锤，同上，待修复后复验** |
| A 约会夜 4 阶段串场 + `markDone()` 修复 | **通过**（4 阶段全走通、13 张卡、结束态正确） |
| A 记忆档案标签 fallback | **通过**（显示「约会夜」，未 fallback 成「情侣棋局」） |
| A 统计写入粒度 | **新发现缺陷**：1 场会话被拆成 4 条记录 |
| C 历史回看 / 无导出分享 | **通过**（在预置数据下验证） |
| 36 模式可达性与返回主页 | 见 §4（脚本实测中，最终数字见文末更新） |
| 防截屏（FLAG_SECURE） | **通过**（screencap rc=1、产物 0 字节） |
| 崩溃 / ANR | 见 §6 |

---

## 1. [CRASH-P0-001] 18+ 年龄确认按钮完全失效（首启白屏）

**状态：P0 待修复后复验**（悠悠已改源码，本轮 APK 未包含修复）

### 现象
清空数据后首启：
1. 弹窗顺序异常：`OnboardingDialogFragment` **盖在** `AgeGateDialogFragment` 之上。用户先看到「开启今晚的微醺氛围 / 写下你们的专属称呼…」，点「继续」→「开始今夜」后才看到「仅限成年人」弹窗。
2. 在「仅限成年人」弹窗点「我已年满 18 岁，继续」→ 弹窗消失，但**主页内容区全空**，只剩底部导航栏（「大厅」等 4 个 tab），应用不可用。
3. `shared_prefs/` 下**没有** `velvet_age_gate.xml`，偏好未写盘。
4. 重启 App → 年龄弹窗再次出现（无限循环）。
5. 点「我未满 18 岁」同样只是关窗，无 declined 说明、无写盘、navHost 仍不可见。

### 实测证据
```
首启 dump（ui_r14_02.xml）：
  '仅限成年人' | tv_agegate_title
  '这是一款面向两位成年人的私密互动应用。请确认你已年满 18 岁再继续。' | tv_agegate_body
  '我未满 18 岁' | button2
  '我已年满 18 岁，继续' | button1

点击肯定按钮后（ui_r14_03.xml）：
  '大厅' | navigation_bar_item_large_label_view     ← 仅剩底栏，内容区空白

shared_prefs 目录（点击前后相同）：
  velvet_couple_profile.xml                          ← 无 velvet_age_gate.xml
```

### 根因（AOSP 源码级定位）
`AgeGateDialogFragment.onStart()` 原实现：
```java
super.onStart();                 // DialogFragment 内部执行 mDialog.show()
                                 //   → sendShowMessage()；此时 mShowMessage == null，什么都不发
dialog.setOnShowListener(d -> {  // 只是把 listener 存进 mShowMessage，不会补发 SHOW 消息
    dialog.getButton(...).setOnClickListener(...);
});
```
AOSP `android.app.Dialog`（android-12.0.0_r1）确认：
```java
public void setOnShowListener(@Nullable OnShowListener listener) {
    if (listener != null) mShowMessage = mListenersHandler.obtainMessage(SHOW, listener);
    else mShowMessage = null;
}
private void sendShowMessage() {
    if (mShowMessage != null) Message.obtain(mShowMessage).sendToTarget();
}
```
⇒ listener 注册发生在 `sendShowMessage()` **之后**，SHOW 消息永远不会投递，按钮 listener 从未挂上；按钮沿用 `setPositiveButton(..., null)` 的默认 auto-dismiss，既不 `confirm()` 也不 `setNavHostVisible(true)`。

### 修复建议（已上报 team-lead，已指派悠悠）
在 `onCreateDialog()` 中直接给 `setPositiveButton/setNegativeButton` 传 listener，删除失效的 `setOnShowListener` 段。源码已于 11-24 10:16 修改，本轮实测 APK 仍为修复前构建。

### 关联
AC-106（零权限门禁不受影响）、架架 §2.1~§2.4 / [API-009]、安安 RED-A2/A7（文案本身合规，实测无身体/动作暗示词）。

---

## 2. [CRASH-P0-002] 今晚一句话：两个保存入口都无效

**状态：P0 待修复后复验**

### 现象
1. 主页「今晚留一句话 → 留下这句」打开编辑弹窗，填写 `NOTE_A_ROUND14` / `NOTE_B_ROUND14` 并选「超棒」。
2. 弹窗内**有两个**「留下这句」按钮：自定义视图内 `btn_tn_save`（y=1060）与对话框正按钮 `button1`（y=1192）。
3. 点 `btn_tn_save` → 弹窗**不关闭**、`velvet_tonight_note.xml` **不存在**（该按钮在代码中无任何 `setOnClickListener`，是死按钮）。
4. 再点对话框正按钮 → 弹窗关闭，但 `velvet_tonight_note.xml` **仍不存在**，数据未写入。因此「同日覆盖写」「历史回看」在真实路径下**无法触发**。

### 根因
与 P0-001 完全相同的范式：`TonightNoteDialogFragment.onStart()` 在 `super.onStart()` 之后才 `setOnShowListener`，listener 永不执行。

### 关联
架架 §3.5 / [API-010]、安安 RED-A6 / RED-P（无导出分享，此项通过）。

---

## 3. A / C 三链路实测

### 3.1 [A] 约会夜串场 —— 通过
测试前用预置 `velvet_age_gate.xml`（confirmed=true）绕过 P0 门禁（**非真实用户路径**）。

| 阶段 | 期望卡数 | 实测进度文本 | 结果 |
|---|---|---|---|
| 暖场 WARM_UP | 4 | 第 1/4 → 2/4 → 3/4 → 4/4 | ✅ |
| 靠近一点 DEEPEN | 4 | 第 1/4 → 2/4 → 3/4 → 4/4 | ✅ |
| 心跳 PEAK | 3 | 第 1/3 → 2/3 → 3/3 | ✅ |
| 慢慢收尾 CLOSING | 2 | 第 1/2 → 2/2 | ✅ |

- **`markDone()` 修复已实测有效**：暖场第 4 张点「开始今晚」后，阶段标题立即由「暖场」变为「靠近一点」，进度重置为「第 1 张，共 4 张」；不再出现 13 张全从暖场池抽取的旧问题。
- PEAK **无倒计时**：`fragment_date_night.xml` 全文无任何计时控件，`DateNightStage` 四阶段 `durationSec` 均为 0，实测 PEAK 页面只有「开始今晚 / 跳过这张 / 安全词随时可用」三个动作，无倒计时文本。**注意**：原任务卡写「其余阶段有倒计时」，实测四阶段都没有；这是规格与实现不一致，需主理人裁决（不影响「PEAK 无倒计时」这条验收）。
- 会话结束：正/副按钮均 `enabled=false`，卡片文本为「今晚就到这里…就停在这一刻吧，哪天想继续，我们再续上。」✅
- 安全暂停条常驻可见：「安全词随时可用 —— 想暂停或停下，随时说。」✅
- 主页再次进入时副标题显示「今晚就到这里…」，当日完成识别有效。

### 3.2 [A-记忆档案] 标签映射通过，写入粒度有缺陷
`session_stats` 实测（sqlite3 查询）：
```
gameMode    playerACompleted  playerBCompleted
DATE_NIGHT  2                 2
DATE_NIGHT  3                 3
DATE_NIGHT  4                 4
DATE_NIGHT  4                 4
```
- ✅ 标签正确：设置页「约会夜记忆档案」显示 4 条 `tv_stat_mode` 均为「**约会夜**」，未 fallback 成「情侣棋局」（第八轮历史坑已修）。`TagDisplayMapper` 的 `"DATE_NIGHT"` 映射生效。
- ❌ **新缺陷 [DATA-DN-001]**：`DateNightFragment.onPrimaryClicked()` 在每个阶段完成时调用 `writeHistory()`，导致 **1 场会话被写成 4 条独立记录**（4/4、4/4、3/3、2/2），记忆档案显示「共度夜晚：4 场 · 累计亲密互动：26 次」——实际只玩了 1 场 13 张。建议改为会话结束时一次性 flush 13/13，或增加 sessionId 合并口径。

### 3.3 [C] 今晚一句话 —— 写入链路阻塞，回看与隐私姿态通过
- ❌ 写入：见 §2（P0-002）。
- ✅ 同日覆盖写：静态确认 `TonightNoteManager.saveNote()` 以 `day_<ISO>` 为键，`putString/putInt/putLong` + `commit()`，同日再写覆盖而非追加（`getNote()`/`getAllNotes()` 按 `KEY_MOOD` 键枚举，一天最多 1 条）。
- ✅ 历史回看（预置 `velvet_tonight_note.xml` 后走真实入口）：主页副标题变为「已留在这台设备上 —— 只有你们两个人看得到。」，按钮变为「留下的那些夜」；点击进入后显示：
  ```
  '留下的那些夜' | tv_tnh_title
  '2026年11月24日' | tv_tn_item_date
  '超棒' | tv_tn_item_mood
  'SEEDED_A_ROUND14  ·  SEEDED_B_ROUND14' | tv_tn_item_summary
  ```
- ✅ 无导出/分享：历史页 resource-id 全集仅 `tv_tnh_title / tv_tnh_empty / rv_tnh_notes / root_tn_item / tv_tn_item_*` + 公共顶栏，布局与代码中 `share|export|导出|分享` 仅出现在注释「无导出 / 分享 / 批量操作」。

---

## 4. 36 个游戏模式可达性回归

（见文末最终数据）

---

## 5. 静态生命周期 / 异步回调审查

- `DateNightFragment`：异步 `contentRepository.loadMergedPoolAsync` 回调内有 `if (!isAdded()) return;` + `runOnUiThread` 内 `if (binding == null) return;`；`onDestroyView()` 中先兜底 `writeHistory()` 再 `binding = null`。**通过**（存在 `requireActivity()` 在极窄 detach 竞态窗口内抛 `IllegalStateException` 的理论风险，未实测复现）。
- `TonightNoteHistoryFragment`：`observeNotes().observe(getViewLifecycleOwner(), ...)` + 回调判 `binding == null` + `onDestroyView()` 置空。**通过**。
- `AgeGateDialogFragment` / `TonightNoteDialogFragment`：`onDestroyView()` 均置空 binding。**通过**；按钮生命周期问题见 §1/§2。
- 同类范式自查：工程内 `DialogFragment` 共 5 个（`PassPhoneDialog`、`OnboardingDialogFragment`、`PaywallDialogFragment` 及上述 2 个）。**仅这 2 个**使用 `setOnShowListener`，其余均在 `onCreateDialog` 内给自定义 View 绑 listener，未发现同类潜伏问题。
- `ConsentFilter` 全局 HARD_NO：`replaceRules()` 同步替换 hard/soft 集合，`isAllowed()` 对任一 tag 命中即拒绝；`isAllowed()` 调用点覆盖 `TaskEngine`、`SocialGamesEngine`、`ProceduralTaskGenerator`、`ModePromptSynthesizer`、`DateNightSessionEngine` 及 12+ 独立游戏引擎（盲选/21点/拍卖/剧本杀/四子棋等）。11 条默认规则完整（`deep_talk/playful/eye_contact/physical_light/bite_lick`=YES，`clothing_dare/restraint_light/blindfold/roleplay/alcohol`=SOFT_LIMIT，`public_space`=HARD_NO）。**通过**。
- 卡池下限：`SocialGamesEngine` 与 `DateNightSessionEngine` 均为 `MAX_RECENT_SHIELD=12` / `MIN_AVAILABLE=4`，shield 计算 `min(12, max(0, size-4))`，任何池至少保留 4 张可用卡。**通过**。
- 倒计时反馈：`TaskPromptDialogHelper` 在 `DareTimerHelper.Listener.onFinish()` 调 `HapticHelper.countdownFinished()`，内部 `Vibrator.vibrate(VibrationEffect.createOneShot(...))` + `ToneGenerator(TONE_PROP_BEEP)`；静音/无震动硬件均静默降级。**通过（静态）**。
- Manifest 权限集合实测仅 `{android.permission.VIBRATE}`，符合 AC-106。

---

## 6. 崩溃 / ANR / logcat 异常

（见文末最终数据）

---

## 7. 运行时 UI 氛围观察（转 android-ui，本人不改布局）

- 约会夜顶栏「⏸ 暂停」按钮 176×96 px，安全暂停条 828×96 px，位置固定在卡片操作区下方，**显眼可信**；点按有 `HapticHelper.tick` 轻触反馈。
- 阶段指示器 4 段（暖场/靠近一点/心跳/慢慢收尾）实测有颜色 + 粗体切换，仪式感明确。
- 揭晓类交互（抽卡、盲选、刮刮卡、21 点）均有触感反馈。
- **待优化 1**：首启弹窗顺序错乱（年龄门禁被 onboarding 盖住），见 §1。
- **待优化 2**：约会夜 PEAK 阶段卡片文案出现「用唇舌与指尖交替挑逗…」「灼热湿吻于…大腿内侧」等高显式措辞，与「留白更多、语言更轻」的阶段设计意图冲突，且可能触及 Play 政策红线；建议转 android-security / android-data 复核（本项与 AC-703 热度梯度相关）。
- **待优化 3**：主页 `tv_mode_title` 在列表底部被底栏裁切至 6px 高（如「亲密默契轮盘」初始帧 `[450,1437][826,1443]`），需确认是否为滚动容器 padding 不足。
