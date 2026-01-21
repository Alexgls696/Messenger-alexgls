const createGroupManager = (() => {
    let modal, closeBtn, createBtn;
    let nameInput, descInput;
    let searchForm, searchInput, searchResults;
    let selectedContainer;

    let localApiBaseUrl;
    let selectedUsers = new Map(); // Map<id, userObject> для уникальности

    let onGroupCreatedCallback = null;

    const close = () => {
        modal.classList.add('hidden');
        resetForm();
    };

    const resetForm = () => {
        nameInput.value = '';
        descInput.value = '';
        searchInput.value = '';
        searchResults.innerHTML = '';
        selectedUsers.clear();
        renderSelectedMembers();
    };

    // Рендерит "чипсы" выбранных пользователей
    const renderSelectedMembers = () => {
        selectedContainer.innerHTML = '';

        if (selectedUsers.size === 0) {
            selectedContainer.innerHTML = '<span class="placeholder-text">Никто не выбран</span>';
            return;
        }

        selectedUsers.forEach(user => {
            const chip = document.createElement('div');
            chip.className = 'selected-member-chip';
            chip.innerHTML = `
                <span>${user.name}</span>
                <button class="remove-member-btn" data-id="${user.id}">&times;</button>
            `;

            chip.querySelector('.remove-member-btn').addEventListener('click', () => {
                selectedUsers.delete(user.id);
                renderSelectedMembers();
                // Если этот пользователь есть в результатах поиска, обновляем кнопку
                updateSearchButtons();
            });

            selectedContainer.appendChild(chip);
        });
    };

    const updateSearchButtons = () => {
        const buttons = searchResults.querySelectorAll('.add-member-btn');
        buttons.forEach(btn => {
            const userId = parseInt(btn.dataset.id);
            if (selectedUsers.has(userId)) {
                btn.textContent = 'Добавлен';
                btn.classList.add('added');
                btn.disabled = true;
            } else {
                btn.textContent = 'Добавить';
                btn.classList.remove('added');
                btn.disabled = false;
            }
        });
    };

    const renderSearchResults = (users) => {
        searchResults.innerHTML = '';
        if (!users || users.length === 0) {
            searchResults.innerHTML = '<p class="placeholder">Ничего не найдено.</p>';
            return;
        }

        users.forEach(user => {
            const div = document.createElement('div');
            div.className = 'user-item';
            div.innerHTML = `
                <img class="user-item-avatar" src="/images/profile-default.png">
                <div class="user-item-info">
                    <div class="user-item-name">${user.name} ${user.surname || ''}</div>
                    <div class="user-item-username">@${user.username}</div>
                </div>
                <button class="add-member-btn" data-id="${user.id}">Добавить</button>
            `;

            const addBtn = div.querySelector('.add-member-btn');
            addBtn.addEventListener('click', () => {
                selectedUsers.set(user.id, user);
                renderSelectedMembers();
                updateSearchButtons();
            });

            searchResults.appendChild(div);

            // Загрузка аватара (как везде)
            const avatarImg = div.querySelector('.user-item-avatar');
            const authToken = localStorage.getItem('accessToken');
            apiFetch(`${localApiBaseUrl}/api/profiles/images/user-avatar/${user.id}`)
                .then(avatarId => {
                    if (avatarId) imageLoader.getImageSrc(avatarId, localApiBaseUrl, authToken).then(src => avatarImg.src = src);
                }).catch(()=>{});
        });

        updateSearchButtons();
    };

    const createGroup = async () => {
        const name = nameInput.value.trim();
        const description = descInput.value.trim();

        if (!name) {
            alert("Введите название группы");
            return;
        }

        const membersIds = Array.from(selectedUsers.keys());
        const payload = { name, description, membersIds };

        try {
            createBtn.disabled = true;
            createBtn.textContent = 'Создание...';

            // Получаем ответ от сервера (новый ChatDto)
            const newGroupChat = await apiFetch(`${localApiBaseUrl}/api/chats/group`, {
                method: 'POST',
                body: JSON.stringify(payload)
            });

            close();

            if (onGroupCreatedCallback) {
                onGroupCreatedCallback(newGroupChat);
            }

        } catch (error) {
            console.error("Ошибка создания группы:", error);
            alert("Не удалось создать группу.");
        } finally {
            createBtn.disabled = false;
            createBtn.textContent = 'Создать';
        }
    };

    const init = (baseUrl,  onGroupCreated) => {
        localApiBaseUrl = baseUrl;
        onGroupCreatedCallback = onGroupCreated;

        modal = document.getElementById('createGroupModal');
        closeBtn = document.getElementById('closeCreateGroupBtn');
        createBtn = document.getElementById('submitCreateGroupBtn');

        nameInput = document.getElementById('groupNameInput');
        descInput = document.getElementById('groupDescInput');

        searchForm = document.getElementById('groupMemberSearchForm');
        searchInput = document.getElementById('groupMemberSearchInput');
        searchResults = document.getElementById('groupMemberSearchResults');

        selectedContainer = document.getElementById('selectedMembersContainer');

        // Обработчики
        closeBtn.addEventListener('click', close);
        modal.addEventListener('click', (e) => { if(e.target === modal) close(); });

        createBtn.addEventListener('click', createGroup);

        searchForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const username = searchInput.value.trim();
            if (!username) return;

            try {
                const users = await apiFetch(`${localApiBaseUrl}/api/search/users/by-username/${username}`);
                renderSearchResults(users);
            } catch (e) {
                searchResults.innerHTML = '<p class="placeholder">Ошибка поиска.</p>';
            }
        });

        // Открытие модалки по кнопке из шапки
        document.getElementById('createGroupBtn').addEventListener('click', () => {
            modal.classList.remove('hidden');
            nameInput.focus();
        });
    };

    return { init };
})();