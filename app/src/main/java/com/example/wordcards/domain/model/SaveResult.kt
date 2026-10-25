package com.example.wordcards.domain.model

/**
 * 新增/编辑的结果。
 *
 * 用 sealed interface 而不是抛异常：唯一键冲突是**预期内**的业务分支
 * （用户重复录入同一个词），用异常表达会逼着上层写 try/catch 去接。
 */
sealed interface SaveResult {

    data class Success(val id: Long) : SaveResult

    /** 单词已存在（忽略大小写）。 */
    data object DuplicateWord : SaveResult

    /** 必填项校验失败，field 决定错误提示显示在哪个输入框下。 */
    data class Invalid(val field: DraftField) : SaveResult

    /** 编辑时目标记录已被删除。 */
    data object NotFound : SaveResult
}
