function deleteProduct(btn: HTMLButtonElement): void {
    const form = btn.parentNode as HTMLFormElement;
    const prodId = (form.querySelector('[name=productId]') as HTMLInputElement).value;
    const csrf = (form.querySelector('[name=_csrf]') as HTMLInputElement).value;

    const productElement = btn.closest('article')!;

    fetch('/admin/product/' + prodId + '/delete', {
        method: 'DELETE',
        headers: { 'X-CSRF-TOKEN': csrf },
    })
        .then((res) => res.json())
        .then((data) => {
            console.log(data);
            productElement.parentNode?.removeChild(productElement);
        })
        .catch((err) => console.log(err));
}
