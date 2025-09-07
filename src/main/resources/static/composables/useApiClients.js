// The composable now accepts dependencies (Vue functions, axios) as arguments.
export function useApiClients({ ref }, axios) {
    const loading = ref(false);
    const clients = ref([]);

    const API_BASE_URL = '/admin/api-clients';

    // This is the function we will consistently use and export.
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
            await fetchClients(); // Correctly calls the internal function
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
            await fetchClients(); // Correctly calls the internal function
        } catch (error) {
            alert('Failed to delete API client.');
            console.error(error);
        }
    };
    
    const updateClientStatus = async (client) => {
        try {
            await axios.put(`${API_BASE_URL}/${client.id}`, client);
            // No alert needed for a simple toggle, the refresh will confirm the change.
        } catch (error) {
            alert('Failed to update client status.');
            console.error(error);
        }
        // Always refetch to get the source of truth from the server.
        await fetchClients();
    };

    // The main app will call fetchClients on menu activation, so no initial call here.

    return {
        loading,
        clients,
        createClient,
        deleteClient,
        updateClientStatus,
        fetchClients // **CRITICAL FIX: Expose the correctly named function**
    };
}
