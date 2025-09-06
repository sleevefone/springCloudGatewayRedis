// The composable now accepts Vue's reactivity functions as arguments.
export function useViewAndForm({ ref, reactive, computed }) {

    // --- View State ---
    const activeView = ref('RouteList'); // RouteList or RouteForm
    const currentComponent = computed(() => activeView.value);

    // --- Form State ---
    const form = reactive({});
    const isEditMode = ref(false);
    const formTitle = computed(() => isEditMode.value ? 'Edit Route' : 'Create Route');

    // --- Methods ---
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

    return {
        // State
        currentComponent,
        form,
        formTitle,
        isEditMode,
        // Methods
        showCreateForm,
        showEditForm,
        showListView,
        addFilterToForm,
        removeFilterFromForm
    };
}
