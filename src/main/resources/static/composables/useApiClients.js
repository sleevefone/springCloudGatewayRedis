// The composable now accepts ElMessage for notifications.
export function useApiClients({ ref }, axios, ElMessage) {
    const loading = ref(false);
    const clients = ref([]);
    const searchQuery = ref('');

    const API_BASE_URL = '/admin/api-clients';

    const fetchClients = async (query = '') => {
        loading.value = true;
        let url = API_BASE_URL;
        if (query) {
            url += `?query=${encodeURIComponent(query)}`;
        }
        try {
            const response = await axios.get(url);
            clients.value = response.data;
        } catch (error) {
            ElMessage.error('Failed to load API clients.');
            console.error(error);
        } finally {
            loading.value = false;
        }
    };

    const handleSearch = () => {
        fetchClients(searchQuery.value);
    };

    const handleReset = () => {
        searchQuery.value = '';
        fetchClients();
    };

    const createClient = async (description) => {
        if (!description) {
            ElMessage.warning('Description cannot be empty.');
            return;
        }
        try {
            await axios.post(API_BASE_URL, { description });
            ElMessage.success('API Client created successfully.');
            await fetchClients();
        } catch (error) {
            ElMessage.error('Failed to create API client.');
            console.error(error);
        }
    };

    const deleteClient = async (id) => {
        // The confirmation is now handled by el-popconfirm in the template.
        try {
            await axios.delete(`${API_BASE_URL}/${id}`);
            ElMessage.success('API Client deleted successfully.');
            await fetchClients(searchQuery.value);
        } catch (error) {
            ElMessage.error('Failed to delete API client.');
            console.error(error);
        }
    };

    const updateClientStatus = async (client) => {
        try {
            await axios.put(`${API_BASE_URL}/${client.id}`, client);
            ElMessage.success(`Client ${client.appKey} has been ${client.enabled ? 'enabled' : 'disabled'}.`);
        } catch (error) {
            ElMessage.error('Failed to update client status.');
            console.error(error);
        }
        // No need to refetch here, as the parent handler will do it.
    };

    return {
        loading,
        clients,
        searchQuery,
        fetchClients,
        handleSearch,
        handleReset,
        createClient,
        deleteClient,
        updateClientStatus
    };
}
