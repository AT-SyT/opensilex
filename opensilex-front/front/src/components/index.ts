import DefaultFooterComponent from './layout/DefaultFooterComponent.vue';
import DefaultHeaderComponent from './layout/DefaultHeaderComponent.vue';
import DefaultLoginComponent from './layout/DefaultLoginComponent.vue';
import DefaultMenuComponent from './layout/DefaultMenuComponent.vue';
import DefaultHomeComponent from './layout/DefaultHomeComponent.vue';
import DefaultNotFoundComponent from './layout/DefaultNotFoundComponent.vue';
import UserForm from './users/UserForm.vue';
import UserList from './users/UserList.vue';
import UserView from './users/UserView.vue';
import ProfileForm from './profiles/ProfileForm.vue';
import ProfileView from './profiles/ProfileView.vue';
import ProfileList from './profiles/ProfileList.vue';
import GroupForm from './groups/GroupForm.vue';
import GroupUserProfileForm from './groups/GroupUserProfileForm.vue';
import GroupView from './groups/GroupView.vue';
import GroupList from './groups/GroupList.vue';
import FormInputLabelHelper from './common/FormInputLabelHelper.vue'
import ToDoComponent from './layout/ToDoComponent.vue';
import InfrastructureView from './infrastructures/InfrastructureView.vue';
import InfrastructureTree from './infrastructures/InfrastructureTree.vue';
import InfrastructureForm from './infrastructures/InfrastructureForm.vue';

import FactorView from './factors/FactorView.vue';
import FactorList from './factors/FactorList.vue';
import FactorForm from './factors/FactorForm.vue';

import FactorLevelView from './factors/FactorLevelView.vue';
import FactorLevelList from './factors/FactorLevelList.vue';
import FactorLevelForm from './factors/FactorLevelForm.vue';

import ExperimentForm from './experiments/ExperimentForm.vue';
import ExperimentForm2 from './experiments/ExperimentForm2.vue';
import ExperimentList from './experiments/ExperimentList.vue';
import ExperimentCreate from './experiments/ExperimentCreate.vue';
import ExperimentView from './experiments/ExperimentView.vue';

export default {
    'opensilex-DefaultFooterComponent': DefaultFooterComponent,
    'opensilex-DefaultHeaderComponent': DefaultHeaderComponent,
    'opensilex-DefaultLoginComponent': DefaultLoginComponent,
    'opensilex-DefaultMenuComponent': DefaultMenuComponent,
    'opensilex-DefaultHomeComponent': DefaultHomeComponent,
    'opensilex-DefaultNotFoundComponent': DefaultNotFoundComponent,
    'opensilex-UserForm': UserForm,
    'opensilex-UserList': UserList,
    'opensilex-UserView': UserView,
    'opensilex-ProfileForm': ProfileForm,
    'opensilex-ProfileView': ProfileView,
    'opensilex-ProfileList': ProfileList,
    'opensilex-GroupForm': GroupForm,
    'opensilex-GroupUserProfileForm': GroupUserProfileForm,
    'opensilex-GroupView': GroupView,
    'opensilex-GroupList': GroupList,

    // infrastructures
    "opensilex-InfrastructureView": InfrastructureView,
    "opensilex-InfrastructureTree": InfrastructureTree,
    "opensilex-InfrastructureForm": InfrastructureForm,
    'opensilex-FormInputLabelHelper': FormInputLabelHelper,
    // factors
    "opensilex-FactorView": FactorView,
    "opensilex-FactorList": FactorList,
    "opensilex-FactorForm": FactorForm,
    // factors levels
    "opensilex-FactorLevelView": FactorLevelView,
    "opensilex-FactorLevelList": FactorLevelList,
    "opensilex-FactorLevelForm": FactorLevelForm,

    // experiments
    "opensilex-core-ExperimentForm": ExperimentForm,
    "opensilex-core-ExperimentForm2": ExperimentForm2,
    "opensilex-core-ExperimentList": ExperimentList,
    "opensilex-core-ExperimentCreate": ExperimentCreate,
    "opensilex-core-ExperimentView": ExperimentView,
    'opensilex-ToDoComponent': ToDoComponent
};