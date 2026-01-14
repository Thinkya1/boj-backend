<template>
  <div class="detail-page">
    <div class="detail-topbar">
      <RouterLink class="back-link" to="/questions">返回题库</RouterLink>
      <div class="detail-title">{{ question?.title || '题目详情' }}</div>
      <div></div>
    </div>

    <div class="detail-layout">
      <section class="detail-left">
        <div class="detail-tabs">
          <button
            v-for="tab in tabs"
            :key="tab"
            class="detail-tab"
            :class="{ active: activeTab === tab }"
            type="button"
            @click="activeTab = tab"
          >
            {{ tab }}
          </button>
        </div>
        <div class="detail-content">
          <template v-if="activeTab === '题目'">
            <div class="problem-title">{{ question?.title || '题目详情' }}</div>
            <div class="problem-meta">
              <span v-if="!displayTags.length" class="tag">基础题</span>
              <span v-if="!displayTags.length" class="tag">算法</span>
              <span v-for="tag in displayTags" :key="tag" class="tag">{{ tag }}</span>
            </div>
            <div class="limit-row">
              <div class="limit-item">
                <span>时间限制</span>
                <strong>{{ timeLimitText }}</strong>
              </div>
              <div class="limit-item">
                <span>内存限制</span>
                <strong>{{ memoryLimitText }}</strong>
              </div>
            </div>
            <div class="stack" style="gap: 18px;">
              <div>
                <div class="section-title" style="font-size: 18px;">题目描述</div>
                <div class="markdown" v-html="renderedContent"></div>
              </div>
              <div v-if="question?.judgeConfig">
                <div class="section-title" style="font-size: 18px;">判题配置</div>
                <div class="grid grid-3" style="margin-top: 8px;">
                  <div>
                    <div class="muted">时间限制</div>
                    <div>{{ question?.judgeConfig?.timeLimit || '-' }} ms</div>
                  </div>
                  <div>
                    <div class="muted">内存限制</div>
                    <div>{{ question?.judgeConfig?.memoryLimit || '-' }} KB</div>
                  </div>
                  <div>
                    <div class="muted">栈限制</div>
                    <div>{{ question?.judgeConfig?.stackLimit || '-' }} KB</div>
                  </div>
                </div>
              </div>
              <div>
                <div class="section-title" style="font-size: 18px;">标准答案</div>
                <div v-if="answerText" class="sample-card">
                  <pre class="sample-box">{{ answerText }}</pre>
                </div>
                <div v-else class="muted">暂无标准答案</div>
              </div>
            </div>
          </template>
          <template v-else>
            <div class="muted">功能建设中，敬请期待。</div>
          </template>
        </div>
      </section>

      <section class="detail-right">
        <div class="editor-shell">
          <div class="editor-toolbar">
            <div>编辑语言</div>
            <select v-model="language" class="select" @change="applyTemplate">
              <option value="java">java</option>
              <option value="gcc">c</option>
              <option value="javascript">javascript</option>
              <option value="python">python</option>
            </select>
          </div>
          <textarea v-model="code" class="code-area" spellcheck="false"></textarea>
          <div class="editor-actions">
            <button class="btn btn-ghost" type="button" @click="reset">重置模板</button>
            <div style="display: flex; gap: 12px; align-items: center;">
              <span v-if="lastSubmissionId" class="hint">提交 ID：{{ lastSubmissionId }}</span>
              <button class="btn btn-primary" :disabled="submitting" type="button" @click="submit">
                {{ submitting ? '提交中...' : '提交答案' }}
              </button>
            </div>
          </div>
        </div>

        <div class="result-shell">
          <div class="result-header">
            <span>执行结果</span>
            <span class="badge" :class="badgeClass(latestSubmission?.status, latestSubmission?.judgeInfo?.message, latestSubmission?.result)">
              {{ formatResult(latestSubmission?.status, latestSubmission?.judgeInfo?.message, latestSubmission?.result).text }}
            </span>
          </div>

          <div v-if="latestSubmission" class="result-grid">
            <div class="result-item">
              <span>执行用时</span>
              <strong>{{ formatTime(latestSubmission?.judgeInfo?.time) }}</strong>
            </div>
            <div class="result-item">
              <span>内存消耗</span>
              <strong>{{ formatMemory(latestSubmission?.judgeInfo?.memory) }}</strong>
            </div>
            <div class="result-item">
              <span>执行结果</span>
              <strong>{{ formatResult(latestSubmission?.status, latestSubmission?.judgeInfo?.message, latestSubmission?.result).text }}</strong>
            </div>
            <div class="result-item">
              <span>判题信息</span>
              <strong>{{ formatJudgeMessage(latestSubmission?.judgeInfo?.message) }}</strong>
            </div>
          </div>
          <div v-else class="muted">暂无提交结果</div>

          <div style="display: flex; justify-content: flex-end;">
            <RouterLink class="btn btn-ghost" to="/submissions">查看提交记录</RouterLink>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { get, post } from '../api/http';
import { useAuthStore } from '../stores/auth';
import { useToastStore } from '../stores/toast';
import { formatJudgeMessage, formatMemory, formatResult, formatTime } from '../utils/format';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const toast = useToastStore();

const tabs = ['题目', '评论', '答案', '提交记录', '提交'];
const activeTab = ref('题目');

const question = ref(null);
const language = ref('java');
const code = ref('');
const submitting = ref(false);
const lastSubmissionId = ref('');
const recentSubmissions = ref([]);
const saveTimer = ref(null);

const templates = {
  java: `import java.io.*;\nimport java.util.*;\n\npublic class Main {\n    public static void main(String[] args) throws Exception {\n        StreamTokenizer in = new StreamTokenizer(new BufferedReader(new InputStreamReader(System.in)));\n        if (in.nextToken() == StreamTokenizer.TT_EOF) {\n            return;\n        }\n        long a = (long) in.nval;\n        if (in.nextToken() == StreamTokenizer.TT_EOF) {\n            return;\n        }\n        long b = (long) in.nval;\n        System.out.print(a + b);\n    }\n}\n`,
  gcc: `#include <stdio.h>\n\nint main() {\n    long long a, b;\n    if (scanf(\"%lld %lld\", &a, &b) != 2) {\n        return 0;\n    }\n    printf(\"%lld\", a + b);\n    return 0;\n}\n`,
  javascript: `const fs = require('fs');\n\nconst data = fs.readFileSync(0, 'utf8').trim();\nif (!data) {\n  process.exit(0);\n}\nconst nums = data.split(/\\s+/).map(Number);\nif (nums.length >= 2) {\n  process.stdout.write(String(nums[0] + nums[1]));\n}\n`,
  python: `import sys\n\n\ndef main():\n    data = sys.stdin.read().strip().split()\n    if len(data) < 2:\n        return\n    a = int(data[0])\n    b = int(data[1])\n    sys.stdout.write(str(a + b))\n\n\nif __name__ == '__main__':\n    main()\n`,
};

const latestSubmission = computed(() => recentSubmissions.value[0] || null);
const displayTags = computed(() => question.value?.tags || []);
const answerText = computed(() => question.value?.answer || '');
const renderedContent = computed(() => renderMarkdown(question.value?.content || ''));
const timeLimitText = computed(() => {
  const value = question.value?.judgeConfig?.timeLimit;
  return value ? `${value} ms` : '-';
});
const memoryLimitText = computed(() => {
  const value = question.value?.judgeConfig?.memoryLimit;
  if (!value) {
    return '-';
  }
  if (value >= 1024) {
    return `${(value / 1024).toFixed(2)} MB`;
  }
  return `${value} KB`;
});

async function fetchQuestion() {
  const data = await get('/question/get/vo', { params: { id: route.params.id } });
  question.value = data;
}

function applyTemplate() {
  loadSavedCode();
}

function reset() {
  code.value = templates[language.value];
  saveCode(code.value);
}

function storageKey(lang = language.value) {
  return `boj_code_${route.params.id}_${lang}`;
}

function loadSavedCode() {
  const saved = localStorage.getItem(storageKey());
  if (saved) {
    code.value = saved;
    return;
  }
  code.value = templates[language.value];
}

function saveCode(value) {
  localStorage.setItem(storageKey(), value);
}

function escapeHtml(value) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function renderMarkdown(value) {
  if (!value) {
    return '';
  }
  const blocks = [];
  let text = value.replace(/\r\n/g, '\n');
  text = text.replace(/```([\s\S]*?)```/g, (match, codeBlock) => {
    const escaped = escapeHtml(codeBlock.trim());
    blocks.push(`<pre><code>${escaped}</code></pre>`);
    return `@@CODEBLOCK_${blocks.length - 1}@@`;
  });
  text = escapeHtml(text);
  text = text.replace(/^### (.*)$/gm, '<h3>$1</h3>');
  text = text.replace(/^## (.*)$/gm, '<h2>$1</h2>');
  text = text.replace(/^# (.*)$/gm, '<h1>$1</h1>');
  text = text.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
  text = text.replace(/`([^`]+)`/g, '<code>$1</code>');
  text = text.replace(/\n{2,}/g, '</p><p>');
  text = `<p>${text.replace(/\n/g, '<br>')}</p>`;
  text = text.replace(/@@CODEBLOCK_(\d+)@@/g, (match, index) => blocks[Number(index)] || '');
  text = text.replace(/<p>(<pre><code>[\s\S]*?<\/code><\/pre>)<\/p>/g, '$1');
  return text;
}

async function submit() {
  if (!auth.user) {
    toast.push('请先登录', 'error');
    return;
  }
  if (!code.value.trim()) {
    toast.push('请填写完整代码', 'error');
    return;
  }
  submitting.value = true;
  try {
    const payload = {
      questionId: route.params.id,
      language: language.value,
      code: code.value,
    };
    const submissionId = await post('/question_submit/', payload);
    lastSubmissionId.value = submissionId;
    toast.push('提交成功', 'info');
    await router.push(`/submissions/${submissionId}`);
  } finally {
    submitting.value = false;
  }
}

async function fetchRecentSubmissions() {
  if (!auth.user) {
    recentSubmissions.value = [];
    return;
  }
  const data = await post('/question_submit/list/page', {
    current: 1,
    pageSize: 5,
    questionId: route.params.id,
    userId: auth.user.id,
    sortField: 'createTime',
    sortOrder: 'descend',
  });
  recentSubmissions.value = data.records || [];
}

function badgeClass(status, message, result) {
  const tone = formatResult(status, message, result).tone;
  if (tone === 'success') return 'badge-success';
  if (tone === 'danger') return 'badge-danger';
  if (tone === 'info') return 'badge-info';
  return 'badge-muted';
}

onMounted(async () => {
  await fetchQuestion();
  loadSavedCode();
  await fetchRecentSubmissions();
});

watch(language, () => {
  loadSavedCode();
});

watch(code, (value) => {
  if (saveTimer.value) {
    clearTimeout(saveTimer.value);
  }
  saveTimer.value = setTimeout(() => saveCode(value), 400);
});

onBeforeUnmount(() => {
  if (saveTimer.value) {
    clearTimeout(saveTimer.value);
  }
});
</script>
