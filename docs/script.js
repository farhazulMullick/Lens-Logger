const lightbox = document.querySelector('.lightbox');
const preview = lightbox.querySelector('img');

document.querySelectorAll('[data-lightbox]').forEach((button) => {
  button.addEventListener('click', () => {
    const image = button.querySelector('img');
    preview.src = button.dataset.lightbox;
    preview.alt = image?.alt ?? 'LensLogger screenshot';
    lightbox.showModal();
  });
});

lightbox.querySelector('.lightbox-close').addEventListener('click', () => lightbox.close());
lightbox.addEventListener('click', (event) => {
  if (event.target === lightbox) lightbox.close();
});
lightbox.addEventListener('close', () => { preview.src = ''; });
