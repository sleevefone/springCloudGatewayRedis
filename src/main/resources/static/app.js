const { createApp, ref, reactive, computed } = Vue;

const API_BASE_URL = '/admin/routes';

// Helper function to create a default empty form state
const createDefaultFormState = () => ({
    id: '',
    uri: 'lb://', // Default to load balanced URI
    order: 0,
    predicatesJson: JSON.stringify([{
        name: 'Path',
        args: { 'patterns': '/example/**' }
    }], null, 2),
    filters: [], // Array of { name, argsJson, enabled }
});

const app = createApp({
    setup() {
        // --- State --- 
        const loading = ref(true);
        const routes = ref([]);
        const dialogVisible = ref(false);
        const isEditMode = ref(false);
        const form = reactive(createDefaultFormState());

        const dialogTitle = computed(() => isEditMode.value ? 'Edit Route' : 'Create Route');

        // --- API Methods ---
        const fetchRoutes = async () => {
            loading.value = true;
            try {
                const response = await axios.get(API_BASE_URL);
                routes.value = response.data;
            } catch (error) {
                ElementPlus.ElMessage.error('Failed to load routes.');
                console.error(error);
            } finally {
                loading.value = false;
            }
        };

        // --- Event Handlers ---
        const handleCreate = () => {
            Object.assign(form, createDefaultFormState()); // Reset form to default
            isEditMode.value = false;
            dialogVisible.value = true;
        };

        const handleEdit = (route) => {
            isEditMode.value = true;
            form.id = route.id;
            form.uri = route.uri;
            form.order = route.order;
            form.predicatesJson = JSON.stringify(route.predicates || [], null, 2);
            // Convert backend FilterInfo to frontend UI model
            form.filters = route.filters.map(f => ({
                name: f.name,
                argsJson: JSON.stringify(f.args || {}, null, 2),
                enabled: f.enabled
            }));
            dialogVisible.value = true;
        };

        const handleDelete = async (id) => {
            try {
                await axios.delete(`${API_BASE_URL}/${id}`);
                ElementPlus.ElMessage.success('Route deleted successfully.');
                fetchRoutes(); // Refresh list
            } catch (error) {
                ElementPlus.ElMessage.error('Failed to delete route.');
                console.error(error);
            }
        };

        const handleSubmit = async () => {
            try {
                // 1. Prepare the payload to match backend DTO (RouteDefinitionPayload)
                const payload = {
                    id: form.id,
                    uri: form.uri,
                    order: form.order,
                    predicates: JSON.parse(form.predicatesJson),
                    // Convert frontend UI model back to backend FilterInfo
                    filters: form.filters.map(f => ({
                        name: f.name,
                        args: JSON.parse(f.argsJson),
                        enabled: f.enabled
                    }))
                };

                // 2. Send request
                if (isEditMode.value) {
                    await axios.put(`${API_BASE_URL}/${form.id}`, payload);
                    ElementPlus.ElMessage.success('Route updated successfully.');
                } else {
                    await axios.post(API_BASE_URL, payload);
                    ElementPlus.ElMessage.success('Route created successfully.');
                }

                // 3. Close dialog and refresh
                dialogVisible.value = false;
                fetchRoutes();
            } catch (error) {
                ElementPlus.ElMessage.error('Failed to save route. Check JSON format and console for details.');
                console.error(error);
            }
        };

        const addFilter = () => {
            form.filters.push({ name: '', argsJson: '{}', enabled: true });
        };

        const removeFilter = (index) => {
            form.filters.splice(index, 1);
        };

        // --- Initial Load ---
        fetchRoutes();

        return {
            loading,
            routes,
            dialogVisible,
            dialogTitle,
            isEditMode,
            form,
            handleCreate,
            handleEdit,
            handleDelete,
            handleSubmit,
            addFilter,
            removeFilter
        };
    }
});

app.use(ElementPlus);
app.mount('#app');
