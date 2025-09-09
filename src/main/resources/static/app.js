import { useRoutes } from './composables/useRoutes.js';
import { useViewAndForm } from './composables/useViewAndForm.js';
import { useApiClients } from './composables/useApiClients.js';
import { useFactories } from './composables/useFactories.js';

window.onload = async function () {
    const { createApp, ref, reactive, computed, onMounted } = Vue;
    const axiosInstance = window.axios;

    const fetchTemplate = async (path) => {
        const response = await fetch(path);
        if (!response.ok) throw new Error(`Failed to fetch template: ${path}`);
        return await response.text();
    };

    const [routeListTemplate, routeFormTemplate, apiClientListTemplate, factoryListTemplate] = await Promise.all([
        fetchTemplate('./components/RouteList.html'),
        fetchTemplate('./components/RouteForm.html'),
        fetchTemplate('./components/ApiClientList.html'),
        fetchTemplate('./components/FactoryList.html')
    ]);

    const RouteList = { template: routeListTemplate, props: ['routes', 'loading', 'searchQuery'], emits: ['create-route', 'edit-route', 'delete-route', 'update:searchQuery', 'query', 'reset', 'toggle-enabled'] };
    const RouteForm = { template: routeFormTemplate, props: ['formData', 'title', 'isEditMode'], emits: ['save-route', 'cancel', 'add-filter', 'remove-filter', 'add-predicate', 'remove-predicate'] };
    const ApiClientList = { template: apiClientListTemplate, props: ['clients', 'loading', 'searchQuery'], emits: ['create-client', 'delete-client', 'update-client-status', 'update:searchQuery', 'query', 'reset'], setup: () => ({ newClientDescription: ref('') }) };
    const FactoryList = { template: factoryListTemplate, props: ['predicates', 'filters', 'loading'] };

    const app = createApp({
        components: { RouteList, RouteForm, ApiClientList, FactoryList },
        setup() {
            const vueDeps = { ref, reactive, computed };
            const routesManager = useRoutes(vueDeps, axiosInstance);
            const viewAndFormManager = useViewAndForm(vueDeps);
            const apiClientsManager = useApiClients(vueDeps, axiosInstance);
            const factoriesManager = useFactories(vueDeps, axiosInstance);

            const activeMenu = ref('routes');

            const selectMenu = async (menu) => {
                activeMenu.value = menu;
                if (menu === 'routes') {
                    await routesManager.fetchRoutes();
                } else if (menu === 'apiClients') {
                    await apiClientsManager.fetchClients();
                } else if (menu === 'factories') {
                    await factoriesManager.fetchFactories();
                }
            };

            const currentComponent = computed(() => {
                if (activeMenu.value === 'routes') return viewAndFormManager.currentComponent.value;
                if (activeMenu.value === 'apiClients') return 'ApiClientList';
                if (activeMenu.value === 'factories') return 'FactoryList';
                return null;
            });

            const handleRouteSubmit = async (formData) => {
                try {
                    const payload = { 
                        ...formData, 
                        predicates: formData.predicates.map(p => ({ name: p.name, args: JSON.parse(p.argsJson || '{}')})), 
                        filters: formData.filters.map(f => ({ ...f, args: JSON.parse(f.argsJson || '{}') })) 
                    };
                    payload.filters.forEach(f => delete f.argsJson);
                    payload.predicates.forEach(p => delete p.argsJson);

                    if (!viewAndFormManager.isEditMode.value && !payload.id) delete payload.id;
                    await axios.post('/__gateway/admin/routes', payload);
                    viewAndFormManager.showListView();
                    await routesManager.fetchRoutes();
                } catch (error) {
                    alert('Failed to save route. Check JSON format.');
                    console.error(error);
                }
            };

            const handleRouteToggle = async (route) => {
                try {
                    const updatedRoute = { ...route, enabled: !route.enabled };
                    await axios.post('/__gateway/admin/routes', updatedRoute);
                    await routesManager.fetchRoutes();
                } catch (error) {
                    alert('Failed to update route status.');
                    console.error(error);
                }
            };

            const handleRouteDelete = async (id) => {
                try {
                    await axios.delete(`/__gateway/admin/routes/${id}`);
                    await routesManager.fetchRoutes();
                } catch (error) {
                    alert('Failed to delete route.');
                    console.error(error);
                }
            };

            onMounted(() => {
                selectMenu('routes');
            });

            return {
                activeMenu, 
                selectMenu,
                currentComponent,
                
                // Route Management
                routes: routesManager.routes,
                routeLoading: routesManager.loading,
                routeSearchQuery: routesManager.searchQuery,
                handleRouteSearch: routesManager.handleSearch,
                handleRouteReset: routesManager.handleReset,
                handleRouteDelete,
                handleRouteToggle,
                
                // Route Form
                form: viewAndFormManager.form,
                formTitle: viewAndFormManager.formTitle,
                isEditMode: viewAndFormManager.isEditMode,
                showCreateForm: viewAndFormManager.showCreateForm,
                showEditForm: viewAndFormManager.showEditForm,
                showListView: viewAndFormManager.showListView,
                addFilterToForm: viewAndFormManager.addFilterToForm,
                removeFilterFromForm: viewAndFormManager.removeFilterFromForm,
                addPredicateToForm: viewAndFormManager.addPredicateToForm,
                removePredicateFromForm: viewAndFormManager.removePredicateFromForm,
                handleRouteSubmit,

                // API Client Management
                clients: apiClientsManager.clients,
                clientLoading: apiClientsManager.loading,
                clientSearchQuery: apiClientsManager.searchQuery,
                handleClientSearch: apiClientsManager.handleSearch,
                handleClientReset: apiClientsManager.handleReset,
                handleClientCreate: apiClientsManager.createClient,
                handleClientDelete: apiClientsManager.deleteClient,
                handleClientUpdateStatus: apiClientsManager.updateClientStatus,

                // Factories View
                predicates: factoriesManager.predicates,
                filters: factoriesManager.filters,
                factoryLoading: factoriesManager.loading,
            };
        }
    });

    app.mount('#app');
};
