// The composable now accepts dependencies (Vue functions, axios) as arguments.
export function useApiClients({ ref }, axios) {
    const loading = ref(false);
    const clients = ref([]);

    const API_BASE_URL = '/admin/api-clients';

    const fetchClients = async () => {
        loading.value = true;
        try {
            const response = await axios.get(API_BASE_URL);
            clients.value = response.data;
        } catch (error) {
            alert('Failed to load API clients.');
            console.error(error);
        } finally {
            loading.value = false;
        }
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
            await fetchClients();
        } catch (error) {
            alert('Failed to delete API client.');
            console.error(error);
        }
    };
    
    const updateClientStatus = async (client) => {
        try {
            await axios.put(`${API_BASE_URL}/${client.id}`, client);
        } catch (error) {
            alert('Failed to update client status.');
            // Revert on failure
            client.enabled = !client.enabled;
            console.error(error);
        }
    };

    // Initial Load
    fetchClients();

    return {
        loading,
        clients,
        createClient,
        deleteClient,
        updateClientStatus
    };
}
