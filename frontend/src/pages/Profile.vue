<template>
  <div class="stack">
    <div class="card">
      <h2 class="section-title">个人中心</h2>
      <p class="muted">维护你的头像与个人介绍。</p>
    </div>

    <div class="card">
      <div class="grid grid-2" style="align-items: center;">
        <div class="stack">
          <label class="stack" style="gap: 6px;">
            <span>昵称</span>
            <input v-model="profile.userName" class="input" placeholder="显示名称" />
          </label>
          <label class="stack" style="gap: 6px;">
            <span>简介</span>
            <textarea v-model="profile.userProfile" class="textarea" placeholder="一句话介绍你"></textarea>
          </label>
          <button class="btn btn-primary" :disabled="saving" @click="saveProfile">
            {{ saving ? '保存中...' : '保存资料' }}
          </button>
        </div>
        <div class="stack" style="align-items: center;">
          <img v-if="profile.userAvatar" :src="profile.userAvatar" class="avatar" style="width: 96px; height: 96px;" alt="avatar" />
          <div class="muted" v-else>暂无头像</div>
          <input type="file" class="input" @change="uploadAvatar" />
          <div class="hint">仅支持 1MB 内的 png/jpg/webp/svg。</div>
        </div>
      </div>
    </div>

    <div class="grid">
      <div class="card">
        <div class="card-header">
          <h3>我的题目</h3>
          <RouterLink class="btn btn-ghost" to="/questions">查看更多</RouterLink>
        </div>
        <div v-if="!myQuestions.length" class="muted">暂无题目</div>
        <div v-else class="stack">
          <div v-for="item in myQuestions" :key="item.id" class="card" style="box-shadow: none;">
            <RouterLink :to="`/questions/${item.id}`" class="section-title" style="font-size: 18px;">
              {{ item.title }}
            </RouterLink>
            <div class="muted">{{ formatDate(item.createTime) }}</div>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <h3>我的帖子</h3>
          <RouterLink class="btn btn-ghost" to="/posts">查看更多</RouterLink>
        </div>
        <div v-if="!myPosts.length" class="muted">暂无帖子</div>
        <div v-else class="stack">
          <div v-for="item in myPosts" :key="item.id" class="card" style="box-shadow: none;">
            <RouterLink :to="`/posts/${item.id}`" class="section-title" style="font-size: 18px;">
              {{ item.title }}
            </RouterLink>
            <div class="muted">{{ formatDate(item.createTime) }}</div>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <h3>我的收藏</h3>
          <RouterLink class="btn btn-ghost" to="/posts">查看更多</RouterLink>
        </div>
        <div v-if="!myFavours.length" class="muted">暂无收藏</div>
        <div v-else class="stack">
          <div v-for="item in myFavours" :key="item.id" class="card" style="box-shadow: none;">
            <RouterLink :to="`/posts/${item.id}`" class="section-title" style="font-size: 18px;">
              {{ item.title }}
            </RouterLink>
            <div class="muted">{{ formatDate(item.createTime) }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useAuthStore } from '../stores/auth';
import { useToastStore } from '../stores/toast';
import { formatDate } from '../utils/format';
import { post, upload } from '../api/http';

const auth = useAuthStore();
const toast = useToastStore();
const saving = ref(false);

const profile = reactive({
  userName: '',
  userAvatar: '',
  userProfile: '',
});

const myQuestions = ref([]);
const myPosts = ref([]);
const myFavours = ref([]);

function syncProfile() {
  profile.userName = auth.user?.userName || '';
  profile.userAvatar = auth.user?.userAvatar || '';
  profile.userProfile = auth.user?.userProfile || '';
}

async function saveProfile() {
  saving.value = true;
  try {
    await auth.updateProfile({
      userName: profile.userName,
      userAvatar: profile.userAvatar,
      userProfile: profile.userProfile,
    });
    toast.push('资料已更新', 'info');
  } finally {
    saving.value = false;
  }
}

async function uploadAvatar(event) {
  const file = event.target.files?.[0];
  if (!file) {
    return;
  }
  const formData = new FormData();
  formData.append('file', file);
  formData.append('biz', 'user_avatar');
  const url = await upload('/file/upload', formData);
  profile.userAvatar = url;
  toast.push('头像上传成功', 'info');
}

async function loadLists() {
  const [questionData, postData, favourData] = await Promise.all([
    post('/question/my/list/page/vo', { current: 1, pageSize: 5, sortField: 'createTime', sortOrder: 'descend' }),
    post('/post/my/list/page/vo', { current: 1, pageSize: 5, sortField: 'createTime', sortOrder: 'descend' }),
    post('/post_favour/my/list/page', { current: 1, pageSize: 5, sortField: 'createTime', sortOrder: 'descend' }),
  ]);
  myQuestions.value = questionData.records || [];
  myPosts.value = postData.records || [];
  myFavours.value = favourData.records || [];
}

onMounted(async () => {
  syncProfile();
  await loadLists();
});
</script>
