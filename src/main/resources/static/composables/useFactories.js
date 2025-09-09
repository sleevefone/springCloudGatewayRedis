export function useFactories({ ref }, axios) {
    const loading = ref(false);
    const predicates = ref([]); // Will be an array of FactoryInfo objects
    const filters = ref([]);    // Will be an array of FactoryInfo objects

    const API_BASE_URL = '/__gateway/admin/factories';

    const fetchFactories = async () => {
        loading.value = true;
        try {
            const response = await axios.get(API_BASE_URL);
            predicates.value = response.data.predicates || [];
            filters.value = response.data.filters || [];
        } catch (error) {
            alert('Failed to load factory lists.');
            console.error(error);
        } finally {
            loading.value = false;
        }
    };

    return {
        loading,
        predicates,
        filters,
        fetchFactories
    };
}
