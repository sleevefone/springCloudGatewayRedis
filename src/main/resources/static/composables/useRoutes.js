// This composable now handles backend-driven search.
export function useRoutes({ ref }) {

    const loading = ref(true);
    const routes = ref([]); // Simplified to a single list, the source of truth is the backend.
    const searchQuery = ref('');

    const API_BASE_URL = '/admin/routes';

    // The core function now accepts an optional query string.
    const fetchRoutes = async (query = '') => {
        loading.value = true;
        let url = API_BASE_URL;
        if (query) {
            url += `?query=${encodeURIComponent(query)}`;
        }
        try {
            const response = await axios.get(url);
            routes.value = response.data;
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
            // After deleting, refresh the current view (it might be a filtered view)
            await fetchRoutes(searchQuery.value);
        } catch (error) {
            alert('Failed to delete route.');
            console.error(error);
        }
    };

    // Search now triggers a backend API call.
    const handleSearch = () => {
        fetchRoutes(searchQuery.value);
    };

    // Reset also triggers a backend API call for the full list.
    const handleReset = () => {
        searchQuery.value = '';
        fetchRoutes();
    };

    // Initial Load - get the full list.
    fetchRoutes();

    return {
        loading,
        routes, // The prop will now be just 'routes'
        searchQuery,
        handleDelete,
        handleSearch,
        handleReset,
        fetchRoutes // Expose for external refresh
    };
}
