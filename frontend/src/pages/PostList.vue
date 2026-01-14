<template>
  <div class="stack">
    <div class="card">
      <div class="card-header">
        <div>
          <h2 class="section-title">讨论区</h2>
          <p class="muted">分享思路、交流解法。</p>
        </div>
        <RouterLink v-if="auth.user" class="btn btn-primary" to="/posts/new">发布帖子</RouterLink>
      </div>
      <div class="grid" style="grid-template-columns: 2fr 1fr 1fr; align-items: end;">
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
            <option value="favourNum">收藏数</option>
          </select>
        </label>
      </div>
      <div class="grid" style="grid-template-columns: 1fr 1fr; margin-top: 12px;">
        <label class="stack" style="gap: 6px;">
          <span>标签匹配方式</span>
          <select v-model="tagMode" class="select">
            <option value="all">全部标签</option>
            <option value="any">任意标签</option>
          </select>
        </label>
        <label class="stack" style="gap: 6px;">
          <span>搜索来源</span>
          <select v-model="searchMode" class="select">
            <option value="db">数据库</option>
            <option value="es">ES 搜索</option>
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
      <div v-else-if="!items.length" class="card">暂无帖子</div>
      <div v-else v-for="item in items" :key="item.id" class="card">
        <div class="card-header">
          <div>
            <RouterLink :to="`/posts/${item.id}`" class="section-title" style="font-size: 20px;">
              {{ item.title }}
            </RouterLink>
            <div class="muted" style="margin-top: 6px;">
              作者：{{ item.user?.userName || '匿名' }} · {{ formatDate(item.createTime) }}
            </div>
          </div>
          <div style="display: flex; gap: 8px;">
            <button v-if="canEdit(item)" class="btn btn-ghost" @click="edit(item)">编辑</button>
            <button v-if="canEdit(item)" class="btn btn-danger" @click="remove(item)">删除</button>
          </div>
        </div>
        <p class="muted" style="white-space: pre-wrap; margin-bottom: 12px;">
          {{ preview(item.content) }}
        </p>
        <div>
          <span v-for="tag in item.tagList || []" :key="tag" class="tag">{{ tag }}</span>
        </div>
        <div style="display: flex; gap: 12px; margin-top: 12px; align-items: center;">
          <button class="btn btn-ghost" @click="toggleThumb(item)">
            {{ item.hasThumb ? '已赞' : '点赞' }} ({{ item.thumbNum || 0 }})
          </button>
          <button class="btn btn-ghost" @click="toggleFavour(item)">
            {{ item.hasFavour ? '已藏' : '收藏' }} ({{ item.favourNum || 0 }})
          </button>
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
import { formatDate, parseTags } from '../utils/format';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const toast = useToastStore();

const items = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(10);
const loading = ref(false);
const keyword = ref('');
const tagsInput = ref('');
const sortField = ref('createTime');
const tagMode = ref('all');
const searchMode = ref('db');

watch(
  () => route.query.q,
  (value) => {
    keyword.value = value ? String(value) : '';
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
    if (keyword.value.trim()) {
      payload.searchText = keyword.value.trim();
    }
    const tags = parseTags(tagsInput.value);
    if (tags.length) {
      if (tagMode.value === 'any') {
        payload.orTags = tags;
      } else {
        payload.tags = tags;
      }
    }
    const path = searchMode.value === 'es' ? '/post/search/page/vo' : '/post/list/page/vo';
    const data = await post(path, payload);
    items.value = data.records || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
}

function applySearch() {
  const query = keyword.value.trim();
  router.push({ query: query ? { q: query } : {} });
}

function resetFilters() {
  keyword.value = '';
  tagsInput.value = '';
  sortField.value = 'createTime';
  tagMode.value = 'all';
  searchMode.value = 'db';
  page.value = 1;
  router.push({ query: {} });
  fetchList();
}

function changePage(next) {
  page.value = next;
  fetchList();
}

function preview(text) {
  if (!text) {
    return '暂无内容';
  }
  return text.length > 120 ? `${text.slice(0, 120)}...` : text;
}

function canEdit(item) {
  if (!auth.user) {
    return false;
  }
  return auth.isAdmin || auth.user.id === item.userId;
}

function edit(item) {
  router.push(`/posts/${item.id}/edit`);
}

async function remove(item) {
  if (!confirm('确认删除该帖子吗？')) {
    return;
  }
  await post('/post/delete', { id: item.id });
  toast.push('删除成功', 'info');
  fetchList();
}

async function toggleThumb(item) {
  if (!auth.user) {
    toast.push('请先登录', 'error');
    return;
  }
  const delta = await post('/post_thumb/', { postId: item.id });
  item.thumbNum = (item.thumbNum || 0) + delta;
  item.hasThumb = !item.hasThumb;
}

async function toggleFavour(item) {
  if (!auth.user) {
    toast.push('请先登录', 'error');
    return;
  }
  const delta = await post('/post_favour/', { postId: item.id });
  item.favourNum = (item.favourNum || 0) + delta;
  item.hasFavour = !item.hasFavour;
}

</script>
