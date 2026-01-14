<template>
  <div class="stack">
    <div class="card submit-filter-card">
      <div class="filter-row">
        <div class="filter-title">查找记录</div>
        <div class="filter-fields">
          <span class="filter-label">题目编号</span>
          <input v-model="filters.questionNumber" class="input input-sm" placeholder="如 1000" />
          <button class="btn btn-primary" @click="apply">搜索</button>
        </div>
      </div>
      <div class="filter-row">
        <div class="filter-title">筛选选项</div>
        <div class="filter-fields">
          <span class="filter-label">记录状态</span>
          <select v-model="filters.status" class="select select-sm">
            <option value="">全部状态</option>
            <option value="0">等待判题</option>
            <option value="1">判题中</option>
            <option value="2">成功</option>
            <option value="3">失败</option>
          </select>
          <span class="filter-label">语言</span>
          <select v-model="filters.language" class="select select-sm">
            <option value="">全部</option>
            <option value="java">Java</option>
            <option value="gcc">C</option>
            <option value="javascript">JavaScript</option>
            <option value="python">Python</option>
          </select>
          <span class="filter-label">范围</span>
          <select v-model="filters.scope" class="select select-sm">
            <option value="mine">仅我的</option>
            <option value="all">全部</option>
          </select>
        </div>
        <button class="btn-link" type="button" @click="reset">清除所有筛选条件</button>
      </div>
      <div class="filter-summary">共计 {{ total }} 条结果</div>
    </div>

    <div class="card" v-if="loading">加载中...</div>
    <div class="card" v-else-if="!items.length">暂无提交记录</div>
    <div v-else class="card submit-list">
      <div v-for="item in items" :key="item.id" class="submit-item" @click="openDetail(item)">
        <div class="submit-user">
          <img
            v-if="item.userVO?.userAvatar"
            :src="item.userVO.userAvatar"
            class="avatar avatar-sm"
            alt="avatar"
          />
          <div v-else class="avatar avatar-sm avatar-fallback">{{ getUserInitial(item) }}</div>
          <div class="submit-user-block">
            <div class="submit-user-name">{{ getUserName(item) }}</div>
            <div class="muted">{{ formatDate(item.createTime) }}</div>
          </div>
        </div>
        <div class="submit-status">
          <span class="badge" :class="badgeClass(item.status, item.judgeInfo?.message, item.result)">
            {{ formatResult(item.status, item.judgeInfo?.message, item.result).text }}
          </span>
        </div>
        <div class="submit-problem-block">
          <RouterLink class="submit-problem" :to="`/questions/${item.questionId}`" @click.stop>
            P{{ item.questionVO?.questionNumber || item.questionId }} {{ item.questionVO?.title || '题目' }}
          </RouterLink>
        </div>
        <div class="submit-meta">
          <div class="metric-line">
            <span>{{ formatTime(item.judgeInfo?.time) }}</span>
            <span>{{ formatMemory(item.judgeInfo?.memory) }}</span>
          </div>
          <div class="muted">{{ formatLanguage(item.language) }}</div>
        </div>
      </div>
    </div>

    <Pagination :page="page" :page-size="pageSize" :total="total" @update:page="changePage" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { post } from '../api/http';
import { useAuthStore } from '../stores/auth';
import Pagination from '../components/Pagination.vue';
import { formatDate, formatLanguage, formatResult, formatTime, formatMemory } from '../utils/format';

const auth = useAuthStore();
const router = useRouter();
const loading = ref(false);
const items = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(10);

const filters = reactive({
  questionNumber: '',
  language: '',
  status: '',
  scope: 'mine',
});

async function fetchList() {
  loading.value = true;
  try {
    const payload = {
      current: page.value,
      pageSize: pageSize.value,
      sortField: 'createTime',
      sortOrder: 'descend',
    };
    if (filters.questionNumber) {
      const parsed = Number(filters.questionNumber);
      if (!Number.isNaN(parsed)) {
        payload.questionNumber = parsed;
      }
    }
    if (filters.language) {
      payload.language = filters.language;
    }
    if (filters.status !== '') {
      payload.status = Number(filters.status);
    }
    if (filters.scope === 'mine') {
      payload.userId = auth.user?.id;
    }
    const data = await post('/question_submit/list/page', payload);
    items.value = data.records || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
}

function apply() {
  page.value = 1;
  fetchList();
}

function reset() {
  filters.questionNumber = '';
  filters.language = '';
  filters.status = '';
  filters.scope = 'mine';
  page.value = 1;
  fetchList();
}

function changePage(next) {
  page.value = next;
  fetchList();
}

function openDetail(item) {
  router.push(`/submissions/${item.id}`);
}

function badgeClass(status, message, result) {
  const tone = formatResult(status, message, result).tone;
  if (tone === 'success') return 'badge-success';
  if (tone === 'danger') return 'badge-danger';
  if (tone === 'info') return 'badge-info';
  return 'badge-muted';
}

function getUserName(item) {
  return item.userVO?.userName || `用户${item.userId || ''}`;
}

function getUserInitial(item) {
  const name = getUserName(item).trim();
  return name ? name.charAt(0).toUpperCase() : 'U';
}

onMounted(fetchList);
</script>
