// The composable now accepts ElMessage for notifications.
export function useRoutes({ ref }, axios, ElMessage) {

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
            ElMessage.error('Failed to load routes.');
            console.error(error);
        } finally {
            loading.value = false;
        }
    };

    const handleDelete = async (id) => {
        // The confirmation is now handled by el-popconfirm in the template.
        try {
            await axios.delete(`${API_BASE_URL}/${id}`);
            ElMessage.success('Route deleted successfully.');
            await fetchRoutes(searchQuery.value);
        } catch (error) {
            ElMessage.error('Failed to delete route.');
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

    // Initial Load is handled by the main app now.

    return {
        loading,
        routes,
        searchQuery,
        fetchRoutes,
        handleDelete,
        handleSearch,
        handleReset,
    };
}
