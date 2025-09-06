window.onload = function () {
    const { createApp, ref, reactive, computed } = Vue;

    const API_BASE_URL = '/admin/routes';

    const createDefaultFormState = () => ({
        id: '',
        uri: 'lb://',
        order: 0,
        enabled: true,
        predicatesJson: JSON.stringify([{
            name: 'Path',
            args: { 'patterns': '/example/**' }
        }], null, 2),
        filters: [],
    });

    const app = createApp({
        setup() {
            // --- State ---
            const activeView = ref('routes'); // 'routes' or 'dashboard'
            const viewMode = ref('list'); // 'list' or 'form'
            const loading = ref(true);
            const routes = ref([]);
            const isEditMode = ref(false);
            const form = reactive(createDefaultFormState());

            const formTitle = computed(() => isEditMode.value ? 'Edit Route' : 'Create Route');

            // --- View Navigation ---
            const selectView = (view) => {
                activeView.value = view;
            };

            // --- API Methods ---
            const fetchRoutes = async () => {
                loading.value = true;
                try {
                    const response = await axios.get(API_BASE_URL);
                    routes.value = response.data;
                } catch (error) {
                    alert('Failed to load routes.');
                    console.error(error);
                } finally {
                    loading.value = false;
                }
            };

            // --- Event Handlers ---
            const showCreateForm = () => {
                Object.assign(form, createDefaultFormState());
                isEditMode.value = false;
                viewMode.value = 'form';
            };

            const showEditForm = (route) => {
                isEditMode.value = true;
                form.id = route.id;
                form.uri = route.uri;
                form.order = route.order;
                form.enabled = route.enabled;
                form.predicatesJson = JSON.stringify(route.predicates || [], null, 2);
                form.filters = JSON.parse(JSON.stringify(route.filters || []));
                viewMode.value = 'form';
            };

            const showListView = () => {
                viewMode.value = 'list';
            };

            const handleDelete = async (id) => {
                if (!confirm('Are you sure to delete this route?')) {
                    return;
                }
                try {
                    await axios.delete(`${API_BASE_URL}/${id}`);
                    alert('Route deleted successfully.');
                    fetchRoutes();
                } catch (error) {
                    alert('Failed to delete route.');
                    console.error(error);
                }
            };

            const handleSubmit = async () => {
                try {
                    const payload = {
                        id: form.id,
                        uri: form.uri,
                        order: form.order,
                        enabled: form.enabled,
                        predicates: JSON.parse(form.predicatesJson),
                        filters: form.filters.map(f => ({
                            name: f.name,
                            args: JSON.parse(f.argsJson || '{}'),
                            enabled: f.enabled
                        }))
                    };

                    if (!isEditMode.value && !payload.id) {
                        delete payload.id;
                    }

                    await axios.post(API_BASE_URL, payload);
                    alert(`Route ${isEditMode.value ? 'updated' : 'created'} successfully.`);
                    
                    showListView();
                    fetchRoutes();
                } catch (error) {
                    alert('Failed to save route. Check JSON format.');
                    console.error(error);
                }
            };
            
            const addFilter = () => {
                if (!form.filters) {
                    form.filters = [];
                }
                form.filters.push({ name: '', argsJson: '{}', enabled: true });
            };

            const removeFilter = (index) => {
                form.filters.splice(index, 1);
            };

            // --- Initial Load ---
            fetchRoutes();

            return {
                activeView,
                selectView,
                viewMode,
                loading,
                routes,
                isEditMode,
                form,
                formTitle,
                showCreateForm,
                showEditForm,
                showListView,
                handleDelete,
                handleSubmit,
                addFilter,
                removeFilter
            };
        }
    });

    app.mount('#app');
};