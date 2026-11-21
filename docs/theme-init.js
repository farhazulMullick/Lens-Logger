(() => {
  const root = document.documentElement;
  try {
    const savedTheme = localStorage.getItem('lenslogger-docs-theme');
    root.dataset.theme = savedTheme === 'light' || savedTheme === 'dark'
      ? savedTheme
      : (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
  } catch {
    root.dataset.theme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
})();
