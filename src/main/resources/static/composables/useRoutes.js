// The composable now accepts dependencies (Vue functions, axios) as arguments.
export function useRoutes({ ref }, axios) {

    const loading = ref(true);
    const routes = ref([]);
    const searchQuery = ref('');

    const API_BASE_URL = '/admin/routes';

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
            await fetchRoutes(searchQuery.value);
        } catch (error) {
            alert('Failed to delete route.');
            console.error(error);
        }
    };

    const handleSearch = () => {
        fetchRoutes(searchQuery.value);
    };

    const handleReset = () => {
        searchQuery.value = '';
        fetchRoutes();
    };

    // Initial Load
    fetchRoutes();

    return {
        loading,
        routes,
        searchQuery,
        handleDelete,
        handleSearch,
        handleReset,
        fetchRoutes
    };
}
