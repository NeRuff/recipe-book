let componentIndex = 0;
let productOptions = [];

async function loadProducts() {
    const search = document.getElementById('productSearch').value;
    const category = document.getElementById('productCategory').value;
    const cooking = document.getElementById('productCooking').value;
    const flags = Array.from(document.querySelectorAll('#products-tab .flag-filters input:checked')).map(cb => cb.value);
    
    let url = '/api/products/search?';
    if (search) url += `name=${encodeURIComponent(search)}&`;
    if (category) url += `category=${encodeURIComponent(category)}&`;
    if (cooking) url += `cookingRequirement=${encodeURIComponent(cooking)}&`;
    flags.forEach(f => url += `flags=${encodeURIComponent(f)}&`);
    
    const res = await fetch(url);
    const products = await res.json();
    renderProducts(products);
}

function renderProducts(products) {
    const container = document.getElementById('productsList');
    container.innerHTML = products.map(p => {
        console.log(`Рендер ${p.name}: создаю ${p.photos?.length || 0} фото`);
        return `
        <div class="card">
            <h3>${escapeHtml(p.name)}</h3>
            <div class="photos-container">
                ${p.photos && p.photos.length ? p.photos.map(photo => `<img src="${photo}" class="product-photo" onerror="this.style.display='none'">`).join('') : '<div class="no-photo">📷 Нет фото</div>'}
            </div>
            <p>🔥 ${p.calories} ккал | 🥩 ${p.proteins}g | 🧈 ${p.fats}g | 🍚 ${p.carbs}g</p>
            <p>📁 ${p.category} | ${p.cookingRequirement}</p>
            <p>🏷️ ${p.flags.join(', ') || 'нет'}</p>
            <div class="card-buttons">
                <button onclick="editProduct(${p.id})">✏️</button>
                <button onclick="deleteProduct(${p.id})">🗑️</button>
            </div>
        </div>
    `}).join('');
}

async function editProduct(id) {
    const res = await fetch(`/api/products/${id}`);
    const p = await res.json();
    document.getElementById('productModalTitle').innerText = 'Редактировать продукт';
    document.getElementById('productId').value = p.id;
    document.getElementById('prodName').value = p.name;
    document.getElementById('prodCalories').value = p.calories;
    document.getElementById('prodProteins').value = p.proteins;
    document.getElementById('prodFats').value = p.fats;
    document.getElementById('prodCarbs').value = p.carbs;
    document.getElementById('prodComposition').value = p.composition || '';
    document.getElementById('prodCategory').value = p.category;
    document.getElementById('prodCooking').value = p.cookingRequirement;
    document.getElementById('prodPhotos').value = (p.photos || []).join(', ');
    const checkboxes = document.querySelectorAll('#productForm input[type="checkbox"]');
    checkboxes.forEach(cb => cb.checked = p.flags.includes(cb.value));
    document.getElementById('productModal').style.display = 'block';
}

function showProductForm() {
    document.getElementById('productModalTitle').innerText = 'Добавить продукт';
    document.getElementById('productForm').reset();
    document.getElementById('productId').value = '';
    document.getElementById('productModal').style.display = 'block';
}

function closeProductModal() {
    document.getElementById('productModal').style.display = 'none';
}

document.getElementById('productForm').onsubmit = async (e) => {
    e.preventDefault();
    const id = document.getElementById('productId').value;
    const flags = Array.from(document.querySelectorAll('#productForm input[type="checkbox"]:checked')).map(cb => cb.value);
    const photos = document.getElementById('prodPhotos').value.split(',').map(s => s.trim()).filter(s => s);
    
    const proteins = parseFloat(document.getElementById('prodProteins').value);
    const fats = parseFloat(document.getElementById('prodFats').value);
    const carbs = parseFloat(document.getElementById('prodCarbs').value);
    
    if (proteins + fats + carbs > 100) {
        alert('Сумма БЖУ не может превышать 100 грамм');
        return;
    }
    
    const data = {
        name: document.getElementById('prodName').value,
        calories: parseFloat(document.getElementById('prodCalories').value),
        proteins: proteins,
        fats: fats,
        carbs: carbs,
        composition: document.getElementById('prodComposition').value || null,
        category: document.getElementById('prodCategory').value,
        cookingRequirement: document.getElementById('prodCooking').value,
        flags: flags,
        photos: photos
    };
    
    const res = await fetch(id ? `/api/products/${id}` : '/api/products', {
        method: id ? 'PUT' : 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(data)
    });
    if (res.ok) {
        closeProductModal();
        loadProducts();
    } else {
        const error = await res.text();
        alert('Ошибка: ' + error);
    }
};

async function deleteProduct(id) {
    const check = await fetch(`/api/products/${id}/check-delete`);
    const result = await check.json();
    if (!result.canDelete) {
        alert(`Невозможно удалить продукт: используется в блюдах с ID ${result.usedInDishes.join(', ')}`);
        return;
    }
    if (confirm('Удалить продукт?')) {
        await fetch(`/api/products/${id}`, {method: 'DELETE'});
        loadProducts();
    }
}

async function loadDishes() {
    const search = document.getElementById('dishSearch').value;
    const category = document.getElementById('dishCategory').value;
    const flags = Array.from(document.querySelectorAll('#dishes-tab .flag-filters input:checked')).map(cb => cb.value);
    
    let url = '/api/dishes/search?';
    if (search) url += `name=${encodeURIComponent(search)}&`;
    if (category) url += `category=${encodeURIComponent(category)}&`;
    flags.forEach(f => url += `flags=${encodeURIComponent(f)}&`);
    
    const res = await fetch(url);
    const dishes = await res.json();
    renderDishes(dishes);
}

function renderDishes(dishes) {
    const container = document.getElementById('dishesList');
    container.innerHTML = dishes.map(d => `
        <div class="card">
            <h3>${escapeHtml(d.name)}</h3>
            <div class="photos-container">
                ${d.photos && d.photos.length ? d.photos.slice(0, 3).map(photo => `<img src="${escapeHtml(photo)}" class="dish-photo" onerror="this.style.display='none'">`).join('') : '<div class="no-photo">📷 Нет фото</div>'}
                ${d.photos && d.photos.length > 3 ? `<span class="more-photos">+${d.photos.length - 3}</span>` : ''}
            </div>
            <p>🍽️ Порция: ${d.portionSize}г</p>
            <p>🔥 ${Math.round(d.calories)} ккал | 🥩 ${d.proteins.toFixed(1)}g | 🧈 ${d.fats.toFixed(1)}g | 🍚 ${d.carbs.toFixed(1)}g</p>
            <p>📁 ${d.category}</p>
            <p>🏷️ ${d.flags.join(', ') || 'нет'}</p>
            <div class="card-buttons">
                <button onclick="editDish(${d.id})">✏️</button>
                <button onclick="deleteDish(${d.id})">🗑️</button>
            </div>
        </div>
    `).join('');
}

async function loadProductsForSelect() {
    const res = await fetch('/api/products');
    productOptions = await res.json();
}

function addComponent() {
    const container = document.getElementById('componentsContainer');
    const idx = componentIndex++;
    const div = document.createElement('div');
    div.className = 'component-row';
    div.innerHTML = `
        <select id="comp_product_${idx}" style="width:200px">
            ${productOptions.map(p => `<option value="${p.id}">${escapeHtml(p.name)}</option>`).join('')}
        </select>
        <input type="number" id="comp_quantity_${idx}" placeholder="Количество (г)" step="0.1" style="width:100px">
        <button type="button" onclick="this.parentElement.remove()">✖</button>
    `;
    container.appendChild(div);
}

function getComponents() {
    const components = [];
    for (let i = 0; i < componentIndex; i++) {
        const productSelect = document.getElementById(`comp_product_${i}`);
        const quantityInput = document.getElementById(`comp_quantity_${i}`);
        if (productSelect && quantityInput && quantityInput.value) {
            components.push({
                productId: parseInt(productSelect.value),
                quantity: parseFloat(quantityInput.value)
            });
        }
    }
    return components;
}

async function showDishForm() {
    componentIndex = 0;
    document.getElementById('dishModalTitle').innerText = 'Добавить блюдо';
    document.getElementById('dishForm').reset();
    document.getElementById('dishId').value = '';
    document.getElementById('componentsContainer').innerHTML = '';
    await loadProductsForSelect();
    addComponent();
    document.getElementById('dishModal').style.display = 'block';
}

async function editDish(id) {
    const res = await fetch(`/api/dishes/${id}`);
    const d = await res.json();
    document.getElementById('dishModalTitle').innerText = 'Редактировать блюдо';
    document.getElementById('dishId').value = d.id;
    document.getElementById('dishName').value = d.name;
    document.getElementById('dishPortion').value = d.portionSize;
    document.getElementById('dishCategorySelect').value = d.category;
    document.getElementById('dishCalories').value = d.calories;
    document.getElementById('dishProteins').value = d.proteins;
    document.getElementById('dishFats').value = d.fats;
    document.getElementById('dishCarbs').value = d.carbs;
    document.getElementById('dishPhotos').value = (d.photos || []).join(', ');
    
    await loadProductsForSelect();
    document.getElementById('componentsContainer').innerHTML = '';
    componentIndex = 0;
    for (const comp of d.components) {
        const idx = componentIndex++;
        const div = document.createElement('div');
        div.className = 'component-row';
        div.innerHTML = `
            <select id="comp_product_${idx}" style="width:200px">
                ${productOptions.map(p => `<option value="${p.id}" ${p.id === comp.product.id ? 'selected' : ''}>${escapeHtml(p.name)}</option>`).join('')}
            </select>
            <input type="number" id="comp_quantity_${idx}" value="${comp.quantity}" placeholder="Количество (г)" step="0.1" style="width:100px">
            <button type="button" onclick="this.parentElement.remove()">✖</button>
        `;
        document.getElementById('componentsContainer').appendChild(div);
    }
    if (d.components.length === 0) addComponent();
    document.getElementById('dishModal').style.display = 'block';
}

function closeDishModal() {
    document.getElementById('dishModal').style.display = 'none';
}

document.getElementById('dishForm').onsubmit = async (e) => {
    e.preventDefault();
    const id = document.getElementById('dishId').value;
    const photos = document.getElementById('dishPhotos').value.split(',').map(s => s.trim()).filter(s => s);
    const components = getComponents();
    
    if (components.length === 0) {
        alert('Добавьте хотя бы один продукт в состав блюда');
        return;
    }
    
    const data = {
        name: document.getElementById('dishName').value,
        portionSize: parseFloat(document.getElementById('dishPortion').value),
        category: document.getElementById('dishCategorySelect').value,
        calories: document.getElementById('dishCalories').value ? parseFloat(document.getElementById('dishCalories').value) : null,
        proteins: document.getElementById('dishProteins').value ? parseFloat(document.getElementById('dishProteins').value) : null,
        fats: document.getElementById('dishFats').value ? parseFloat(document.getElementById('dishFats').value) : null,
        carbs: document.getElementById('dishCarbs').value ? parseFloat(document.getElementById('dishCarbs').value) : null,
        components: components,
        photos: photos,
        flags: []
    };
    
    const res = await fetch(id ? `/api/dishes/${id}` : '/api/dishes', {
        method: id ? 'PUT' : 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(data)
    });
    if (res.ok) {
        closeDishModal();
        loadDishes();
    } else {
        const error = await res.text();
        alert('Ошибка: ' + error);
    }
};

async function deleteDish(id) {
    if (confirm('Удалить блюдо?')) {
        await fetch(`/api/dishes/${id}`, {method: 'DELETE'});
        loadDishes();
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>]/g, function(m) {
        if (m === '&') return '&amp;';
        if (m === '<') return '&lt;';
        if (m === '>') return '&gt;';
        return m;
    });
}

document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(tc => tc.classList.remove('active'));
        btn.classList.add('active');
        document.getElementById(btn.dataset.tab + '-tab').classList.add('active');
        if (btn.dataset.tab === 'products') loadProducts();
        else loadDishes();
    });
});

loadProducts();