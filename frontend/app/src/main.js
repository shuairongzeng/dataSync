import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router' // Assuming router.js will be created in the same directory

const app = createApp(App)

app.use(ElementPlus)
app.use(router)

app.mount('#app')
