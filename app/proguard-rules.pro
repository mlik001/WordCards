# 发布包默认不混淆（isMinifyEnabled = false）。
# 若后续开启 R8，请注意：
# - Room 生成的实现类由 Room 自带 keep 规则覆盖，无需手写
# - WorkManager 的 Worker 子类需保留无参构造，R8 规则已由 androidx 提供
