package io.github.aw1y2z.sesame.model.normal.answerAI;

import java.util.List;

/**
 * AI 答题服务商统一接口（移植自 Sesame-GR）。
 * <p>Sesame-M 原只有 {@code CustomAI} 一个实现，本接口用于让 Gemini / 通义千问 / 自定义
 * 三类服务商可互换，{@code AnswerAI} 只依赖本接口。
 */
public interface AnswerAIInterface {

    /**
     * 获取AI回答结果
     *
     * @param text 问题内容
     * @return AI回答结果
     */
    String getAnswerStr(String text);

    /**
     * 获取AI答案
     *
     * @return 命中选项的下标；未获取到返回 -1
     */
    Integer getAnswer(String title, List<String> answerList);

    static AnswerAIInterface getInstance() {
        return new AnswerAIInterface() {
            @Override
            public String getAnswerStr(String text) {
                return "";
            }

            @Override
            public Integer getAnswer(String title, List<String> answerList) {
                return -1;
            }
        };
    }
}
