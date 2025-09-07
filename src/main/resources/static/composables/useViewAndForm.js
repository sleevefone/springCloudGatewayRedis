export function useViewAndForm({ ref, reactive, computed }) {

    const activeView = ref('RouteList');
    const currentComponent = computed(() => activeView.value);

    const form = reactive({});
    const isEditMode = ref(false);
    const formTitle = computed(() => isEditMode.value ? 'Edit Route' : 'Create Route');

    const showCreateForm = () => {
        isEditMode.value = false;
        Object.assign(form, {
            id: '', uri: 'lb://', order: 0, enabled: true,
            predicateDescription: '',
            filterDescription: '',
            predicates: [],
            filters: []
        });
        activeView.value = 'RouteForm';
    };

    const showEditForm = (route) => {
        isEditMode.value = true;
        const routeCopy = JSON.parse(JSON.stringify(route));
        Object.assign(form, {
            ...routeCopy,
            predicateDescription: routeCopy.predicateDescription || '',
            filterDescription: routeCopy.filterDescription || '',
            predicates: (routeCopy.predicates || []).map(p => ({ ...p, argsJson: JSON.stringify(p.args || {}, null, 2) })),
            filters: (routeCopy.filters || []).map(f => ({ ...f, argsJson: JSON.stringify(f.args || {}, null, 2) }))
        });
        activeView.value = 'RouteForm';
    };

    const showListView = () => {
        activeView.value = 'RouteList';
    };

    const addPredicateToForm = () => {
        if (!form.predicates) form.predicates = [];
        form.predicates.push({ name: 'Path', argsJson: '{"patterns": ["/example/**"]}' });
    };

    const removePredicateFromForm = (index) => {
        form.predicates.splice(index, 1);
    };

    const addFilterToForm = () => {
        if (!form.filters) form.filters = [];
        form.filters.push({ name: '', argsJson: '{}', enabled: true });
    };

    const removeFilterFromForm = (index) => {
        form.filters.splice(index, 1);
    };

    return {
        currentComponent,
        form,
        formTitle,
        isEditMode,
        showCreateForm,
        showEditForm,
        showListView,
        addPredicateToForm,
        removePredicateFromForm,
        addFilterToForm,
        removeFilterFromForm
    };
}
