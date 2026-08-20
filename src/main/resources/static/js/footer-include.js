// Load a shared HTML fragment and inject into #site-footer
(function(){
    const id = 'site-footer';
    const container = document.getElementById(id);
    if(!container) return;
    fetch('/includes/footer.html', {cache: 'no-store'})
        .then(resp => {
            if(!resp.ok) throw new Error('Failed to load footer');
            return resp.text();
        })
        .then(html => { container.innerHTML = html; })
        .catch(err => { console.warn('Footer include failed:', err); });
})();
