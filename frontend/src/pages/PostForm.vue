<template>
  <div class="stack">
    <div class="card">
      <h2 class="section-title">{{ isEdit ? '编辑帖子' : '发布帖子' }}</h2>
      <p class="muted">表达思路、分享经验。</p>
    </div>

    <form class="stack" @submit.prevent="submit">
      <div class="card stack">
        <label class="stack" style="gap: 6px;">
          <span>标题</span>
          <input v-model="form.title" class="input" placeholder="帖子标题" />
        </label>
        <label class="stack" style="gap: 6px;">
          <span>标签</span>
          <TagInput v-model="form.tags" />
        </label>
        <label class="stack" style="gap: 6px;">
          <span>内容</span>
          <textarea v-model="form.content" class="textarea" placeholder="写下完整内容"></textarea>
        </label>
      </div>
      <div style="display: flex; gap: 12px;">
        <button class="btn btn-primary" :disabled="loading" type="submit">{{ loading ? '保存中...' : '保存' }}</button>
        <button class="btn btn-ghost" type="button" @click="cancel">取消</button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { get, post } from '../api/http';
import { useToastStore } from '../stores/toast';
import TagInput from '../components/TagInput.vue';

const route = useRoute();
const router = useRouter();
const toast = useToastStore();
const loading = ref(false);

const form = reactive({
  id: null,
  title: '',
  content: '',
  tags: [],
});

const isEdit = computed(() => Boolean(route.params.id));

async function loadPost() {
  if (!isEdit.value) {
    return;
  }
  const data = await get('/post/get/vo', { params: { id: route.params.id } });
  form.id = data.id;
  form.title = data.title || '';
  form.content = data.content || '';
  form.tags = data.tagList || [];
}

async function submit() {
  if (!form.title || !form.content) {
    toast.push('请填写标题与内容', 'error');
    return;
  }
  loading.value = true;
  try {
    const payload = {
      id: form.id,
      title: form.title,
      content: form.content,
      tags: form.tags,
    };
    if (isEdit.value) {
      await post('/post/edit', payload);
    } else {
      await post('/post/add', payload);
    }
    toast.push('保存成功', 'info');
    router.push('/posts');
  } finally {
    loading.value = false;
  }
}

function cancel() {
  router.back();
}

onMounted(loadPost);
</script>
