const DEFAULT_REDIRECT = '/'

/**
 * 只允许跳转到当前站点内的路径，避免登录 redirect 被利用为开放重定向。
 */
export const getSafeRedirect = (value: unknown, fallback = DEFAULT_REDIRECT) => {
  const candidate = Array.isArray(value) ? value[0] : value
  if (typeof candidate !== 'string' || !candidate.trim()) {
    return fallback
  }

  try {
    const url = new URL(candidate, window.location.origin)
    if (url.origin !== window.location.origin) {
      return fallback
    }

    const redirect = `${url.pathname}${url.search}${url.hash}`
    return redirect.startsWith('/user/login') ? fallback : redirect
  } catch {
    return fallback
  }
}
