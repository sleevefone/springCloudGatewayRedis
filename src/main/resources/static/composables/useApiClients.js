// The composable now accepts dependencies and has full search capabilities.
export function useApiClients({ ref }, axios) {
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
            alert('Failed to load API clients.');
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
            alert('Description cannot be empty.');
            return;
        }
        try {
            await axios.post(API_BASE_URL, { description });
            alert('API Client created successfully.');
            await fetchClients();
        } catch (error) {
            alert('Failed to create API client.');
            console.error(error);
        }
    };

    const deleteClient = async (id) => {
        if (!confirm('Are you sure to delete this API client? This is irreversible.')) return;
        try {
            await axios.delete(`${API_BASE_URL}/${id}`);
            alert('API Client deleted successfully.');
            await fetchClients(searchQuery.value); // Refresh current view
        } catch (error) {
            alert('Failed to delete API client.');
            console.error(error);
        }
    };
    
    const updateClientStatus = async (client) => {
        try {
            const updatedClient = { ...client, enabled: !client.enabled };
            await axios.put(`${API_BASE_URL}/${client.id}`, updatedClient);
        } catch (error) {
            alert('Failed to update client status.');
            console.error(error);
        }
        await fetchClients(searchQuery.value); // Refresh current view
    };

    return {
        loading,
        clients,
        searchQuery,
        fetchClients, // **CRITICAL FIX: Expose the fetchClients function**
        handleSearch,
        handleReset,
        createClient,
        deleteClient,
        updateClientStatus
    };
}
