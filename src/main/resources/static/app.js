window.onload = async function () {
    const { createApp, ref, reactive, computed } = Vue;

    // --- Robust Component Loading ---
    const fetchTemplate = async (path) => {
        const response = await fetch(path);
        if (!response.ok) throw new Error(`Failed to fetch template: ${path}`);
        return await response.text();
    };

    const [routeListTemplate, routeFormTemplate] = await Promise.all([
        fetchTemplate('./components/RouteList.html'),
        fetchTemplate('./components/RouteForm.html')
    ]);

    const RouteList = {
        template: routeListTemplate,
        props: ['routes', 'loading', 'searchQuery'],
        emits: ['create-route', 'edit-route', 'delete-route', 'update:searchQuery', 'query', 'reset'],
    };

    const RouteForm = {
        template: routeFormTemplate,
        props: ['formData', 'title', 'isEditMode'],
        emits: ['save-route', 'cancel', 'add-filter', 'remove-filter'],
    };

    // --- Main App Definition ---
    const app = createApp({
        components: {
            RouteList,
            RouteForm,
        },
        setup() {
            // --- State ---
            const activeView = ref('RouteList');
            const loading = ref(true);
            const masterRoutes = ref([]); // Stores the original, unfiltered list from the server
            const displayedRoutes = ref([]); // Stores the list currently shown to the user
            const form = reactive({});
            const isEditMode = ref(false);
            const searchQuery = ref('');

            const formTitle = computed(() => isEditMode.value ? 'Edit Route' : 'Create Route');
            const currentComponent = computed(() => activeView.value);

            // --- Search/Query Logic ---
            const handleSearch = () => {
                if (!searchQuery.value) {
                    displayedRoutes.value = masterRoutes.value;
                    return;
                }
                const lowerCaseQuery = searchQuery.value.toLowerCase();
                displayedRoutes.value = masterRoutes.value.filter(route => 
                    route.id.toLowerCase().includes(lowerCaseQuery) || 
                    route.uri.toLowerCase().includes(lowerCaseQuery)
                );
            };

            const handleReset = () => {
                searchQuery.value = '';
                displayedRoutes.value = masterRoutes.value;
            };

            // --- API Methods ---
            const API_BASE_URL = '/admin/routes';

            const fetchRoutes = async () => {
                loading.value = true;
                try {
                    const response = await axios.get(API_BASE_URL);
                    masterRoutes.value = response.data;
                    handleReset(); // Initialize the displayed list
                } catch (error) {
                    alert('Failed to load routes.');
                    console.error(error);
                } finally {
                    loading.value = false;
                }
            };

            const handleDelete = async (id) => {
                if (!confirm('Are you sure to delete this route?')) return;
                try {
                    await axios.delete(`${API_BASE_URL}/${id}`);
                    alert('Route deleted successfully.');
                    fetchRoutes(); // Refetch and reset the list
                } catch (error) {
                    alert('Failed to delete route.');
                    console.error(error);
                }
            };

            const handleSubmit = async (formData) => {
                try {
                    const payload = {
                        ...formData,
                        predicates: JSON.parse(formData.predicatesJson),
                        filters: formData.filters.map(f => ({ ...f, args: JSON.parse(f.argsJson || '{}') }))
                    };
                    
                    delete payload.predicatesJson;
                    payload.filters.forEach(f => delete f.argsJson);

                    if (!isEditMode.value && !payload.id) delete payload.id;

                    await axios.post(API_BASE_URL, payload);
                    alert(`Route ${isEditMode.value ? 'updated' : 'created'} successfully.`);
                    
                    activeView.value = 'RouteList';
                    fetchRoutes();
                } catch (error) {
                    alert('Failed to save route. Check JSON format.');
                    console.error(error);
                }
            };

            // --- Event Handlers ---
            const showCreateForm = () => {
                isEditMode.value = false;
                Object.assign(form, {
                    id: '', uri: 'lb://', order: 0, enabled: true,
                    predicatesJson: JSON.stringify([{ name: 'Path', args: { 'patterns': '/example/**' } }], null, 2),
                    filters: []
                });
                activeView.value = 'RouteForm';
            };

            const showEditForm = (route) => {
                isEditMode.value = true;
                const routeCopy = JSON.parse(JSON.stringify(route));
                Object.assign(form, {
                    ...routeCopy,
                    predicatesJson: JSON.stringify(routeCopy.predicates || [], null, 2),
                    filters: (routeCopy.filters || []).map(f => ({ ...f, argsJson: JSON.stringify(f.args || {}, null, 2) }))
                });
                activeView.value = 'RouteForm';
            };

            const showListView = () => {
                activeView.value = 'RouteList';
            };
            
            const addFilterToForm = () => {
                if (!form.filters) form.filters = [];
                form.filters.push({ name: '', argsJson: '{}', enabled: true });
            };

            const removeFilterFromForm = (index) => {
                form.filters.splice(index, 1);
            };

            // --- Initial Load ---
            fetchRoutes();

            return {
                // Views
                currentComponent,
                // Data
                loading,
                displayedRoutes, // Pass the displayed list to the component
                form,
                formTitle,
                isEditMode,
                searchQuery,
                // Methods
                handleDelete,
                handleSubmit,
                handleSearch,
                handleReset,
                showCreateForm,
                showEditForm,
                showListView,
                addFilterToForm,
                removeFilterFromForm
            };
        }
    });

    app.mount('#app');
};
