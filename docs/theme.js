(() => {
  const root = document.documentElement;
  const toggle = document.querySelector('.theme-toggle');
  if (!toggle) return;

  const renderToggle = () => {
    const isDark = root.dataset.theme === 'dark';
    const targetMode = isDark ? 'light' : 'dark';
    toggle.setAttribute('aria-pressed', String(isDark));
    toggle.setAttribute('aria-label', `Switch to ${targetMode} mode`);
    toggle.title = `Switch to ${targetMode} mode`;
    toggle.innerHTML = `<i class="doc-icon" data-lucide="${isDark ? 'sun' : 'moon'}" aria-hidden="true"></i><span>${isDark ? 'Light' : 'Dark'}</span>`;
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', isDark ? '#17151b' : '#f8f5fc');
    window.lucide?.createIcons({ attrs: { 'stroke-width': 1.8 } });
  };

  toggle.addEventListener('click', () => {
    const nextTheme = root.dataset.theme === 'dark' ? 'light' : 'dark';
    root.dataset.theme = nextTheme;
    try {
      localStorage.setItem('lenslogger-docs-theme', nextTheme);
    } catch {
      // The theme still works for this page view when storage is unavailable.
    }
    renderToggle();
  });

  renderToggle();
})();
