<template>
  <div class="result-page">
    <div class="result-header">
      <RouterLink class="back-link" to="/submissions">返回提交记录</RouterLink>
      <div class="result-title">提交详情 #{{ submissionId }}</div>
      <div></div>
    </div>

    <div class="result-tabs">
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

    <div class="result-content">
      <div v-if="loading" class="card">加载中...</div>
      <div v-else-if="!submission" class="card">未找到提交记录</div>
      <template v-else>
          <div class="card result-summary">
            <div>
              <div class="muted">题号</div>
              <div>{{ submission.questionVO?.questionNumber || submission.questionId }}</div>
            </div>
          <div>
            <div class="muted">语言</div>
            <div>{{ formatLanguage(submission.language) }}</div>
          </div>
          <div>
            <div class="muted">状态</div>
            <span class="badge" :class="badgeClass(submission.status, submission.judgeInfo?.message, submission.result)">
              {{ formatResult(submission.status, submission.judgeInfo?.message, submission.result).text }}
            </span>
          </div>
            <div>
              <div class="muted">耗时 / 内存</div>
              <div>{{ formatTime(submission.judgeInfo?.time) }} / {{ formatMemory(submission.judgeInfo?.memory) }}</div>
            </div>
            <div>
              <div class="muted">测试点通过</div>
              <div>{{ passedCount }} / {{ caseCount }}</div>
            </div>
          </div>

          <template v-if="activeTab === '测试点信息'">
            <div class="case-summary">
              <span>总计 {{ caseCount }} 个测试点</span>
              <span>通过率 {{ passRate }}</span>
              <span v-if="firstFailIndex">首个未通过：#{{ firstFailIndex }}</span>
            </div>
            <div class="case-grid">
              <div
                v-for="caseResult in caseResults"
                :key="caseResult.index"
                class="case-item"
                :class="caseResult.status === 'AC' ? 'case-pass' : 'case-fail'"
              >
                <div class="case-title">#{{ caseResult.index }}</div>
                <div class="case-status">{{ caseResult.status }}</div>
                <div class="case-meta">
                  <span>{{ caseResult.time ? formatTime(caseResult.time) : '--' }}</span>
                  <span>{{ caseResult.memory ? formatMemory(caseResult.memory) : '--' }}</span>
                </div>
              </div>
            </div>
            <div v-if="!caseResults.length" class="muted">暂无测试点详情</div>
          </template>

          <template v-else>
            <div class="card">
              <div class="card-header">
                <div class="section-title" style="font-size: 16px;">源代码</div>
                <button class="btn btn-ghost" type="button" @click="copySource">复制</button>
              </div>
              <pre class="code-area code-view">{{ submission.code || '暂无源码权限' }}</pre>
            </div>
          </template>
        </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { post } from '../api/http';
import { useAuthStore } from '../stores/auth';
import { useToastStore } from '../stores/toast';
import { formatLanguage, formatMemory, formatResult, formatTime } from '../utils/format';

const route = useRoute();
const auth = useAuthStore();
const toast = useToastStore();
const submissionId = String(route.params.id);

const tabs = ['测试点信息', '源代码'];
const activeTab = ref('测试点信息');

const submission = ref(null);
const loading = ref(false);
const pollTimer = ref(null);
const polling = ref(false);
const pollAttempts = ref(0);
const maxPollAttempts = 30;
const playedSuccess = ref(false);

const caseResults = computed(() => submission.value?.judgeInfo?.caseResults || []);
const caseCount = computed(() => caseResults.value.length);
const passedCount = computed(() => caseResults.value.filter((item) => item.status === 'AC').length);
const passRate = computed(() => {
  if (!caseCount.value) {
    return '-';
  }
  return `${Math.round((passedCount.value / caseCount.value) * 100)}%`;
});
const firstFailIndex = computed(() => {
  const failed = caseResults.value.find((item) => item.status !== 'AC');
  return failed ? failed.index : null;
});

async function fetchSubmission() {
  if (!auth.user) {
    return;
  }
  loading.value = true;
  try {
    const data = await post('/question_submit/list/page', {
      current: 1,
      pageSize: 50,
      userId: auth.user.id,
      sortField: 'createTime',
      sortOrder: 'descend',
    });
    const match = (data.records || []).find((item) => String(item.id) === submissionId);
    submission.value = match || null;
    maybePlaySuccess();
  } finally {
    loading.value = false;
  }
}

function badgeClass(status, message, result) {
  const tone = formatResult(status, message, result).tone;
  if (tone === 'success') return 'badge-success';
  if (tone === 'danger') return 'badge-danger';
  if (tone === 'info') return 'badge-info';
  return 'badge-muted';
}

function shouldPoll() {
  if (!submission.value) {
    return true;
  }
  return submission.value.status === 0 || submission.value.status === 1;
}

function maybePlaySuccess() {
  if (!submission.value || playedSuccess.value) {
    return;
  }
  const tone = formatResult(submission.value.status, submission.value.judgeInfo?.message, submission.value.result).tone;
  if (tone === 'success') {
    playedSuccess.value = true;
    playSuccessTone();
  }
}

function playSuccessTone() {
  try {
    const AudioCtx = window.AudioContext || window.webkitAudioContext;
    if (!AudioCtx) {
      return;
    }
    const ctx = new AudioCtx();
    const gain = ctx.createGain();
    gain.gain.value = 0.12;
    gain.connect(ctx.destination);

    const osc1 = ctx.createOscillator();
    osc1.type = 'sine';
    osc1.frequency.value = 660;
    osc1.connect(gain);
    osc1.start();
    osc1.stop(ctx.currentTime + 0.12);

    const osc2 = ctx.createOscillator();
    osc2.type = 'sine';
    osc2.frequency.value = 880;
    osc2.connect(gain);
    osc2.start(ctx.currentTime + 0.12);
    osc2.stop(ctx.currentTime + 0.28);
  } catch (error) {
    // ignore audio errors
  }
}

function startPolling() {
  stopPolling();
  pollAttempts.value = 0;
  pollTimer.value = setInterval(async () => {
    if (polling.value) {
      return;
    }
    polling.value = true;
    try {
      await fetchSubmission();
    } finally {
      polling.value = false;
    }
    pollAttempts.value += 1;
    if (!shouldPoll() || pollAttempts.value >= maxPollAttempts) {
      stopPolling();
    }
  }, 1500);
}

function stopPolling() {
  if (pollTimer.value) {
    clearInterval(pollTimer.value);
    pollTimer.value = null;
  }
}

async function copySource() {
  const content = submission.value?.code;
  if (!content) {
    toast.push('暂无可复制代码', 'error');
    return;
  }
  try {
    await navigator.clipboard.writeText(content);
    toast.push('已复制', 'info');
  } catch (error) {
    const textarea = document.createElement('textarea');
    textarea.value = content;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand('copy');
    document.body.removeChild(textarea);
    toast.push('已复制', 'info');
  }
}

onMounted(async () => {
  await fetchSubmission();
  if (shouldPoll()) {
    startPolling();
  }
});

onBeforeUnmount(() => {
  stopPolling();
});
</script>
