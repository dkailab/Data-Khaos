<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <el-icon :size="36" color="#409eff"><DataAnalysis /></el-icon>
        <h2>Data Khaos 数据治理平台</h2>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-switch v-model="captchaEnabled" active-text="开启验证码" inline-prompt />
        </el-form-item>
        <el-form-item v-if="captchaEnabled" prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" placeholder="请输入验证码" :prefix-icon="Key" clearable />
            <img v-if="captchaImg" class="captcha-img" :src="captchaImg" alt="验证码" title="点击刷新" @click="loadCaptcha" />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock, Key } from '@element-plus/icons-vue'
import { getCaptcha } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import type { LoginRequest } from '@/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const captchaEnabled = ref(true)
const captchaImg = ref('')
const form = reactive<LoginRequest>({
  username: '',
  password: '',
  captchaId: '',
  captchaCode: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

async function loadCaptcha() {
  const data = await getCaptcha()
  form.captchaId = data.captchaId
  captchaImg.value = data.imageBase64
}

async function handleLogin() {
  await formRef.value?.validate()
  loading.value = true
  try {
    const payload: LoginRequest = {
      username: form.username,
      password: form.password,
    }
    if (captchaEnabled.value) {
      payload.captchaId = form.captchaId
      payload.captchaCode = form.captchaCode
    }
    await userStore.login(payload)
    ElMessage.success('登录成功')
    router.push((route.query.redirect as string) || '/dashboard')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (captchaEnabled.value) {
    loadCaptcha()
  }
})
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f3a63 0%, #2b5876 50%, #4e4376 100%);
}
.login-card {
  width: 400px;
  padding: 40px 36px 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}
.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 24px;
}
.login-header h2 {
  margin: 12px 0 0;
  font-size: 20px;
  color: #303133;
}
.captcha-row {
  display: flex;
  width: 100%;
  gap: 10px;
}
.captcha-img {
  width: 120px;
  height: 40px;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #dcdfe6;
}
.login-btn {
  width: 100%;
}
</style>
