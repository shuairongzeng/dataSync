<script setup lang="ts">
import { useI18n } from "vue-i18n";
import { ref, reactive, computed } from "vue";
import Motion from "./utils/motion";
import { message } from "@/utils/message";
import { loginRules } from "./utils/rule";
import type { FormInstance } from "element-plus";
import { $t, transformI18n } from "@/plugins/i18n";
import { useUserStoreHook } from "@/store/modules/user";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import { useRouter } from "vue-router";
import { bg, avatar, illustration } from "./utils/static";
import { useDataThemeChange } from "@/layout/hooks/useDataThemeChange";
import { initRouter, getTopMenu } from "@/router/utils";
import { storageLocal } from "@pureadmin/utils";
import { useImageVerify } from "@/components/ReImageVerify/src/hooks";

// 导入登录组件
import LoginPhone from "./components/LoginPhone.vue";
import LoginQrCode from "./components/LoginQrCode.vue";
import LoginRegist from "./components/LoginRegist.vue";
import LoginUpdate from "./components/LoginUpdate.vue";

// 导入图标
import User from "~icons/ri/user-3-fill";
import Lock from "~icons/ri/lock-fill";
import { IconifyIconOnline } from "@/components/ReIcon";

const { t } = useI18n();
const router = useRouter();
const loading = ref(false);
const ruleFormRef = ref<FormInstance>();

const { dataTheme, dataThemeChange } = useDataThemeChange();
dataThemeChange();

// 当前页面：0-账号密码登录，1-手机号登录，2-二维码登录，3-注册，4-忘记密码
const currentPage = computed(() => useUserStoreHook().currentPage);

// 登录表单数据
const ruleForm = reactive({
  username: "admin",
  password: "admin123",
  verifyCode: "9527"
});

// 验证码相关
const isDev = import.meta.env.DEV;
const { domRef, imgCode, getImgCode } = useImageVerify(100, 40);

// 开发环境使用固定验证码
const devVerifyCode = "9527";

// 登录
const onLogin = async (formEl: FormInstance | undefined) => {
  loading.value = true;
  if (!formEl) return;

  await formEl.validate(valid => {
    if (valid) {
      // 验证验证码
      const expectedCode = isDev ? devVerifyCode : imgCode.value;
      if (ruleForm.verifyCode.toLowerCase() !== expectedCode.toLowerCase()) {
        message(isDev ? `开发环境请输入: ${devVerifyCode}` : "验证码错误", { type: "error" });
        if (!isDev) {
          getImgCode(); // 生产环境重新生成验证码
        }
        loading.value = false;
        return;
      }

      useUserStoreHook()
        .loginByUsername({
          username: ruleForm.username,
          password: ruleForm.password
        })
        .then(res => {
          if (res.success) {
            storageLocal().removeItem("async-routes");
            initRouter().then(() => {
              router.push(getTopMenu(true).path);
              message(transformI18n($t("login.pureLoginSuccess")), {
                type: "success"
              });
            });
          }
        })
        .catch(error => {
          console.error("登录失败：", error);
          message(error.response?.data?.error || "登录失败，请重试", {
            type: "error"
          });
          getImgCode(); // 重新生成验证码
        })
        .finally(() => {
          loading.value = false;
        });
    } else {
      loading.value = false;
    }
  });
};

// 页面标题
const title = computed(() => {
  return transformI18n($t("login.pureLogin"));
});

// 开发环境自动填充验证码
const autoFillVerifyCode = () => {
  if (isDev) {
    ruleForm.verifyCode = devVerifyCode;
  }
};
</script>

<template>
  <div class="select-none">
    <img :src="bg" class="wave" />
    <div class="flex-c absolute right-5 top-3">
      <!-- 主题 -->
      <el-switch
        v-model="dataTheme"
        inline-prompt
        :active-icon="useRenderIcon('ri:moon-clear-fill')"
        :inactive-icon="useRenderIcon('ep:sunny')"
        @change="dataThemeChange"
      />
    </div>
    <div class="login-container">
      <div class="img">
        <component :is="illustration" />
      </div>
      <div class="login-box">
        <div class="login-form">
          <avatar class="avatar" />
          <Motion>
            <h2 class="outline-none">{{ title }}</h2>
          </Motion>

          <!-- 登录方式切换 -->
          <Motion :delay="100">
            <div class="login-tabs-container">
              <div class="login-tabs">
                <span
                  :class="`login-word ${currentPage === 0 ? 'checked' : ''}`"
                  @click="useUserStoreHook().SET_CURRENTPAGE(0)"
                >
                  {{ t("login.pureAccountLogin") }}
                </span>
                <div class="login-icons">
                  <span
                    :class="`login-icon ${currentPage === 1 ? 'checked' : ''}`"
                    @click="useUserStoreHook().SET_CURRENTPAGE(1)"
                    :title="t('login.purePhoneLogin')"
                  >
                    <IconifyIconOnline
                      :icon="`ri:iphone-fill`"
                      width="18"
                    />
                  </span>
                  <span
                    :class="`login-icon ${currentPage === 2 ? 'checked' : ''}`"
                    @click="useUserStoreHook().SET_CURRENTPAGE(2)"
                    :title="t('login.pureQRCodeLogin')"
                  >
                    <IconifyIconOnline
                      :icon="`ri:qr-code-line`"
                      width="18"
                    />
                  </span>
                </div>
              </div>
            </div>
          </Motion>

          <!-- 账号密码登录 -->
          <div v-show="currentPage === 0">
            <el-form
              ref="ruleFormRef"
              :model="ruleForm"
              :rules="loginRules"
              size="large"
            >
              <Motion :delay="100">
                <el-form-item
                  :rules="[
                    {
                      required: true,
                      message: transformI18n($t('login.pureUsernameReg')),
                      trigger: 'blur'
                    }
                  ]"
                  prop="username"
                >
                  <el-input
                    v-model="ruleForm.username"
                    clearable
                    :placeholder="t('login.pureUsername')"
                    :prefix-icon="useRenderIcon(User)"
                  />
                </el-form-item>
              </Motion>

              <Motion :delay="150">
                <el-form-item prop="password">
                  <el-input
                    v-model="ruleForm.password"
                    clearable
                    show-password
                    :placeholder="t('login.purePassword')"
                    :prefix-icon="useRenderIcon(Lock)"
                  />
                </el-form-item>
              </Motion>

              <Motion :delay="200">
                <el-form-item prop="verifyCode">
                  <div class="w-full flex justify-between">
                    <el-input
                      v-model="ruleForm.verifyCode"
                      clearable
                      :placeholder="isDev ? `开发环境请输入: ${devVerifyCode}` : t('login.pureVerifyCode')"
                      class="flex-1"
                    />
                    <!-- 开发环境显示固定验证码，生产环境显示 canvas 验证码 -->
                    <div
                      v-if="isDev"
                      class="ml-2 flex items-center justify-center border border-gray-300 rounded bg-gray-100 text-lg font-bold text-blue-600 cursor-pointer hover:bg-blue-50"
                      style="width: 100px; height: 40px"
                      @click="autoFillVerifyCode"
                      title="点击自动填充验证码"
                    >
                      {{ devVerifyCode }}
                    </div>
                    <canvas
                      v-else
                      ref="domRef"
                      class="ml-2 cursor-pointer border border-gray-300 rounded"
                      width="100"
                      height="40"
                      style="width: 100px; height: 40px"
                      @click="getImgCode"
                    />
                  </div>
                </el-form-item>
              </Motion>

              <Motion :delay="250">
                <el-form-item>
                  <div class="w-full h-[20px] flex justify-between items-center">
                    <el-checkbox>
                      {{ t("login.pureRememberPassword") }}
                    </el-checkbox>
                    <el-button
                      link
                      type="primary"
                      @click="useUserStoreHook().SET_CURRENTPAGE(4)"
                    >
                      {{ t("login.pureForgetPassword") }}
                    </el-button>
                  </div>
                </el-form-item>
              </Motion>

              <Motion :delay="300">
                <el-form-item>
                  <el-button
                    class="w-full"
                    size="default"
                    type="primary"
                    :loading="loading"
                    @click="onLogin(ruleFormRef)"
                  >
                    {{ t("login.pureLogin") }}
                  </el-button>
                </el-form-item>
              </Motion>

              <Motion :delay="350">
                <el-form-item>
                  <div class="w-full h-[20px] flex justify-between items-center">
                    <el-button
                      class="w-full"
                      size="default"
                      @click="useUserStoreHook().SET_CURRENTPAGE(3)"
                    >
                      {{ t("login.pureRegister") }}
                    </el-button>
                  </div>
                </el-form-item>
              </Motion>
            </el-form>
          </div>

          <!-- 手机号登录 -->
          <LoginPhone v-if="currentPage === 1" />
          <!-- 二维码登录 -->
          <LoginQrCode v-if="currentPage === 2" />
          <!-- 注册 -->
          <LoginRegist v-if="currentPage === 3" />
          <!-- 忘记密码 -->
          <LoginUpdate v-if="currentPage === 4" />
        </div>
      </div>
    </div>
    <div
      class="w-full flex-c absolute bottom-3 text-sm text-[rgba(0,0,0,0.6)] dark:text-[rgba(220,220,242,0.8)]"
    >
      Copyright © 2020-present
      <a
        class="hover:text-primary!"
        href="https://github.com/pure-admin"
        target="_blank"
      >
        &nbsp;{{ title }}
      </a>
    </div>
  </div>
</template>

<style scoped>
@import url("@/style/login.css");
</style>

<style lang="scss" scoped>
:deep(.el-input-group__append, .el-input-group__prepend) {
  padding: 0;
}

.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: space-evenly;
  align-items: center;
  overflow: hidden;
}

.img {
  width: 63%;
}

.login-box {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100vh;
}

.login-form {
  width: 420px;
  padding: 50px 35px 45px;
  border-radius: 10px;
  background: #fff;
  box-shadow: rgb(0 0 0 / 10%) 0 2px 10px 2px;
}

.dark .login-form {
  background: #1d1e1f;
}

.avatar {
  width: 350px;
  height: 80px;
  display: block;
  margin: 0 auto 25px;
}

h2 {
  margin: 0 0 25px;
  color: #505458;
  font-weight: bold;
  font-size: 26px;
  text-align: center;
}

.dark h2 {
  color: #fff;
}

.login-word {
  padding: 8px 16px;
  color: #606266;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  border-radius: 8px;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.login-word:hover {
  color: #409eff;
  background-color: #ecf5ff;
  border-color: #b3d8ff;
}

.login-word.checked {
  color: #fff;
  background: linear-gradient(135deg, #409eff 0%, #1890ff 100%);
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
  border-color: #409eff;
}

.login-tabs-container {
  margin-bottom: 20px;
}

.login-tabs {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px;
  background: #f8f9fa;
  border-radius: 12px;
  border: 1px solid #e9ecef;
}

.dark .login-tabs {
  background: #2d2d2d;
  border-color: #404040;
}

.login-icons {
  display: flex;
  gap: 8px;
}

.login-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  color: #8c8c8c;
  border: 1px solid transparent;
}

.login-icon:hover {
  color: #409eff;
  background-color: #ecf5ff;
  border-color: #b3d8ff;
}

.login-icon.checked {
  color: #fff;
  background: linear-gradient(135deg, #409eff 0%, #1890ff 100%);
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.3);
  border-color: #409eff;
}

.wave {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: url("@/assets/login/bg.png") no-repeat;
  background-size: cover;
  z-index: -1;
}

@media screen and (max-width: 1200px) {
  .login-container {
    justify-content: center;
  }

  .img {
    display: none;
  }
}
</style>