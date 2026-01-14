<template>
  <div class="stack">
    <div class="card">
      <div class="card-header">
        <div>
      <h2 class="section-title">{{ postItem?.title }}</h2>
      <p class="muted">作者：{{ postItem?.user?.userName || '匿名' }} · {{ formatDate(postItem?.createTime) }}</p>
        </div>
        <div style="display: flex; gap: 8px;">
          <button v-if="canEdit" class="btn btn-ghost" @click="edit">编辑</button>
          <button v-if="canEdit" class="btn btn-danger" @click="remove">删除</button>
        </div>
      </div>
      <div style="margin-bottom: 12px;">
    <span v-for="tag in postItem?.tagList || []" :key="tag" class="tag">{{ tag }}</span>
      </div>
      <p style="white-space: pre-wrap; line-height: 1.7;">{{ postItem?.content }}</p>
      <div style="display: flex; gap: 12px; margin-top: 16px;">
      <button class="btn btn-ghost" @click="toggleThumb">
        {{ postItem?.hasThumb ? '已赞' : '点赞' }} ({{ postItem?.thumbNum || 0 }})
      </button>
      <button class="btn btn-ghost" @click="toggleFavour">
        {{ postItem?.hasFavour ? '已藏' : '收藏' }} ({{ postItem?.favourNum || 0 }})
      </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { get, post } from '../api/http';
import { useAuthStore } from '../stores/auth';
import { useToastStore } from '../stores/toast';
import { formatDate } from '../utils/format';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const toast = useToastStore();

const postItem = ref(null);
const canEdit = computed(() => {
  if (!auth.user || !postItem.value) {
    return false;
  }
  return auth.isAdmin || auth.user.id === postItem.value.userId;
});

async function fetchPost() {
  const data = await get('/post/get/vo', { params: { id: route.params.id } });
  postItem.value = data;
}

async function toggleThumb() {
  if (!auth.user) {
    toast.push('请先登录', 'error');
    return;
  }
  const delta = await post('/post_thumb/', { postId: postItem.value.id });
  postItem.value.thumbNum = (postItem.value.thumbNum || 0) + delta;
  postItem.value.hasThumb = !postItem.value.hasThumb;
}

async function toggleFavour() {
  if (!auth.user) {
    toast.push('请先登录', 'error');
    return;
  }
  const delta = await post('/post_favour/', { postId: postItem.value.id });
  postItem.value.favourNum = (postItem.value.favourNum || 0) + delta;
  postItem.value.hasFavour = !postItem.value.hasFavour;
}

function edit() {
  router.push(`/posts/${route.params.id}/edit`);
}

async function remove() {
  if (!confirm('确认删除该帖子吗？')) {
    return;
  }
  await post('/post/delete', { id: postItem.value.id });
  toast.push('删除成功', 'info');
  router.push('/posts');
}

onMounted(fetchPost);
</script>
