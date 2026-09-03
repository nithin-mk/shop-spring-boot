function deleteProduct(btn) {
    const form = btn.parentNode;
    const prodId = form.querySelector('[name=productId]').value;
    const csrf = form.querySelector('[name=_csrf]').value;

    const productElement = btn.closest('article');

    fetch('/admin/product/' + prodId + '/delete', {
        method: 'DELETE',
        headers: { 'X-CSRF-TOKEN': csrf }
    })
        .then(res => res.json())
        .then(data => {
            console.log(data);
            productElement.parentNode.removeChild(productElement);
        })
        .catch(err => console.log(err));
}
