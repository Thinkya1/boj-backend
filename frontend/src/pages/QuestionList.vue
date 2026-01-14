<template>
  <div class="stack">
    <div class="card">
      <div class="card-header">
        <div>
          <h2 class="section-title">题库</h2>
          <p class="muted">筛选标签与关键词，快速找到题目。</p>
        </div>
        <RouterLink v-if="auth.user" class="btn btn-primary" to="/questions/new">创建题目</RouterLink>
      </div>
      <div class="grid question-filters">
        <label class="stack" style="gap: 6px;">
          <span>题号</span>
          <input v-model="questionNumber" class="input" placeholder="如 1000" />
        </label>
        <label class="stack" style="gap: 6px;">
          <span>关键词</span>
          <input v-model="keyword" class="input" placeholder="标题 / 内容" />
        </label>
        <label class="stack" style="gap: 6px;">
          <span>标签</span>
          <input v-model="tagsInput" class="input" placeholder="多标签用逗号分隔" />
        </label>
        <label class="stack" style="gap: 6px;">
          <span>排序</span>
          <select v-model="sortField" class="select">
            <option value="createTime">最新</option>
            <option value="thumbNum">点赞数</option>
            <option value="submitNum">提交数</option>
            <option value="acceptedNum">通过数</option>
          </select>
        </label>
      </div>
      <div style="margin-top: 16px; display: flex; gap: 12px;">
        <button class="btn btn-primary" @click="applySearch">搜索</button>
        <button class="btn btn-ghost" @click="resetFilters">重置</button>
      </div>
    </div>

    <div class="grid">
      <div v-if="loading" class="card">加载中...</div>
      <div v-else-if="!items.length" class="card">暂无题目</div>
      <div v-else v-for="item in items" :key="item.id" class="card compact-card question-card" style="animation: fadeUp 0.5s ease;">
        <div class="question-row">
          <div class="question-left">
            <RouterLink :to="`/questions/${item.id}`" class="question-title">
              {{ item.title }}
            </RouterLink>
            <div class="question-id">题号 {{ item.questionNumber || item.id }}</div>
          </div>
          <div class="question-right">
            <div class="question-stats">
              <span class="stat-pill">提交 {{ item.submitNum || 0 }}</span>
              <span class="stat-pill stat-success">通过 {{ item.acceptedNum || 0 }}</span>
            </div>
            <div v-if="canEdit(item)" class="question-actions">
              <button class="btn btn-ghost" @click="edit(item)">编辑</button>
              <button class="btn btn-danger" @click="remove(item)">删除</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <Pagination :page="page" :page-size="pageSize" :total="total" @update:page="changePage" />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { post } from '../api/http';
import { useAuthStore } from '../stores/auth';
import { useToastStore } from '../stores/toast';
import Pagination from '../components/Pagination.vue';
import { parseTags } from '../utils/format';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const toast = useToastStore();

const items = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(10);
const loading = ref(false);
const questionNumber = ref('');
const keyword = ref('');
const tagsInput = ref('');
const sortField = ref('createTime');

watch(
  () => [route.query.q, route.query.no],
  ([q, no]) => {
    keyword.value = q ? String(q) : '';
    questionNumber.value = no ? String(no) : '';
    page.value = 1;
    fetchList();
  },
  { immediate: true }
);

async function fetchList() {
  loading.value = true;
  try {
    const payload = {
      current: page.value,
      pageSize: pageSize.value,
      sortField: sortField.value,
      sortOrder: 'descend',
    };
    if (questionNumber.value.trim()) {
      const parsed = Number(questionNumber.value);
      if (!Number.isNaN(parsed)) {
        payload.questionNumber = parsed;
      }
    }
    if (keyword.value.trim()) {
      payload.title = keyword.value.trim();
      payload.content = keyword.value.trim();
    }
    const tags = parseTags(tagsInput.value);
    if (tags.length) {
      payload.tags = tags;
    }
    const data = await post('/question/list/page/vo', payload);
    items.value = data.records || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
}

function applySearch() {
  const query = keyword.value.trim();
  const numberQuery = questionNumber.value.trim();
  const nextQuery = {};
  if (query) {
    nextQuery.q = query;
  }
  if (numberQuery) {
    nextQuery.no = numberQuery;
  }
  router.push({ query: nextQuery });
}

function resetFilters() {
  questionNumber.value = '';
  keyword.value = '';
  tagsInput.value = '';
  sortField.value = 'createTime';
  page.value = 1;
  router.push({ query: {} });
  fetchList();
}

function changePage(next) {
  page.value = next;
  fetchList();
}

function canEdit(item) {
  if (!auth.user) {
    return false;
  }
  return auth.isAdmin || auth.user.id === item.userId;
}

function edit(item) {
  router.push(`/questions/${item.id}/edit`);
}

async function remove(item) {
  if (!confirm('确认删除该题目吗？')) {
    return;
  }
  await post('/question/delete', { id: item.id });
  toast.push('删除成功', 'info');
  fetchList();
}

</script>
