package org.triplea.java;

/**
 * Represents a specialized {@code Consumer<T>} interface for views to implement to listen to
 * changes on their view model. The view model will call the {@code viewModelChanged} method when
 * the view model state has changed in a way where the view should also be updated.
 *
 * <p>Pattern References:
 *
 * <ul>
 *   <li>Presentation model pattern: https://martinfowler.com/eaaDev/PresentationModel.html -
 *   <li>Separated presentation pattern: https://martinfowler.com/eaaDev/SeparatedPresentation.html
 * </ul>
 *
 * Example implementation of a UI class implementing a {@code ViewModelListener}:
 *
 * <pre>{@code
 * class MyUiClass implements ViewModelListener<MyViewModel> {
 *    MyUiClass(MyViewModel viewModel) {
 *        viewModel.setView(this);
 *        TextField userNameField = new TextField();
 *        DocumentListener.addKeyTypedListener(
 *            usernameField, value -> viewModel.setUsername(value));
 *
 *        CheckBox userIsEnabledCheckBox = new CheckBox();
 *        userIsEnabledCheckBox.addActionListener(
 *            checkBoxSelected -> viewModel.setUserIsEnabled(checkboxSelected);
 *    }
 *
 *    @Override
 *    void viewModelChanged(ViewModelT viewModel) {
 *       userName.setText(viewModel.getUsername());
 *       userIsEnabledCheckBox.setSelected(viewModel.getUserIsEnabled());
 *    }
 * }
 * }</pre>
 *
 * @param <ViewModelT> The type of view model, view model listeners should generally be 1:1 to a
 *     view model.
 */
public interface ViewModelListener<ViewModelT> {
  /**
   * Called when view model state is updated and the view needs to also be updated. An
   * implementation is expected to query the {@param viewModel} via 'getter' methods and update
   * any/all UI components.
   *
   * @param viewModel A reference to the updated view model with latest state. Note, this parameter
   *     is for convenience, we expect a view to probably already have a reference to the view
   *     model. We pass the view model as an argument in this method so that the view is not forced
   *     to keep a reference to its view model and can use the parameter provided to query for
   *     updated state.
   */
  void viewModelChanged(ViewModelT viewModel);
}
