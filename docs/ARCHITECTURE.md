# Architecture
This project follows the **MVVM (Model-View-ViewModel)** architecture pattern. The diagram below
shows how different components interact with each other as a unit.

<div align="center">
  <img src="./diagram_architecture_raw.svg" alt="Project architecture diagram"/>
  <p><sub><i>Project architecture diagram</i></sub></p>
</div>

## Table of Contents
<details>
  <summary>Click to expand</summary>  
  <div id="user-content-toc">
    <ul>
      <li><a href="#architecture">1. Architecture</a></li>
      <li>
        <a href="#view-layer">2. View Layer</a>
        <ul>
          <li><a href="#data-exchange-between-fragments">2.1. Data Exchange Between Fragments</a></li>
          <li><a href="#displaying-chart">2.2. Displaying Chart</a></li>
        </ul>
      </li>
      <li>
        <a href="#viewmodel-layer">3. ViewModel Layer</a>
        <ul>
          <li><a href="#managing-ui-state">3.1. Managing UI State</a></li>
          <li><a href="#data-synchronization">3.2. Data Synchronization</a></li>
        </ul>
      </li>
      <li>
        <a href="#model-layer">4. Model Layer</a>
        <ul>
          <li><a href="#entities">4.1. Entities</a></li>
        </ul>
      </li>
    </ul>
  </div>
</details>

## View Layer
The **View Layer** (managed by `MainActivity` or `Fragment`) is responsible for displaying UI
components, handling fragment navigation, and interacting with the `WebView` to render HTML. User
interactions are propagated to the `ViewModel`, which processes and stores the results as a `State`
in `LiveData`. The `Fragment` observes the `LiveData` to update the UI based on changes in the
`State`.

### Data Exchange Between Fragments
Each `Fragment` uses [`FragmentResultKey`](../app/src/main/java/com/robifr/ledger/ui/common/navigation/FragmentResultKey.kt),
an interface primarily implemented by enums, to ensure unique keys for communication. It provides a
`key()` method, which is used in various ways to manage data exchange between fragments:
- In `Bundle.putString()`, `Bundle.putInt()`, and similar methods to pass fragment results or
  arguments between fragments.
- In `Fragment.parentFragmentManager.setFragmentResult()` to return a fragment result along with a
  `Bundle`.
- In `Fragment.parentFragmentManager.setFragmentResultListener()` to listen for results from other
  fragments.
- In `SavedStateHandle.get()` to retrieve an argument from a previous fragment.

That way, whenever we want to exchange data between fragments, we can simply check their
`FragmentResultKey` implementations, without worrying about key clashes.

### Displaying Chart  
[D3.js](https://github.com/d3/d3) is used as the primary library for rendering charts. The
JavaScript files, located in [`assets/chart/`](../app/src/main/assets/chart), handle the chart logic
and visualization.  

To enable interaction between Kotlin and JavaScript, we implement a binding layer in 
[`assetbinding/chart/`](../app/src/main/java/com/robifr/ledger/assetbinding/chart). Each binding
maps a JavaScript function to its Kotlin counterpart. JavaScript interacts with Kotlin via the
[`JsInterface`](../app/src/main/java/com/robifr/ledger/assetbinding/JsInterface.kt) class,
registered using `WebView.addJavascriptInterface()` for secure communication.

## ViewModel Layer
The **ViewModel Layer** acts as a bridge between the **View Layer** and the **Model Layer**. It
handles presentation logic and prepares data to be displayed in the UI. `ViewModel` are
lifecycle-aware, which means they can persist through configuration changes, such as when the device
is rotated, and continue to manage UI-related data without being recreated.

### Managing UI State
To effectively manage data observed by the UI, this project uses:
- [`SafeLiveData`](../app/src/main/java/com/robifr/ledger/ui/common/state/SafeLiveData.kt): A
  null-safe wrapper around `LiveData` for handling persistent and reactive UI data.
- [`UiEvent`](../app/src/main/java/com/robifr/ledger/ui/common/state/UiEvent.kt): A generic state
  wrapper for one-time operation like displaying a snackbar, toast, etc.

Both `SafeLiveData` and `UiEvent` are used to represent the UI's `State`, reflecting its current
status — such as text, colors, or other UI properties.

### Data Synchronization
The [`ModelChangedListener`](../app/src/main/java/com/robifr/ledger/repository/ModelChangedListener.kt)
is registered with the `Repository` to listen for changes in the `LocalDatabase`. Whenever data
changes, the `Repository` notifies all registered listeners.

The [`ModelSyncListener`](../app/src/main/java/com/robifr/ledger/repository/ModelChangedListener.kt)
is usually used alongside [`ModelSynchronizer`](../app/src/main/java/com/robifr/ledger/data/ModelSynchronizer.kt)
or [`InfoSynchronizer`](../app/src/main/java/com/robifr/ledger/data/InfoSynchronizer.kt) to handle
data updates automatically. This eliminates the need to manually override `ModelChangedListener`
methods for each implementation.

## Model Layer
The **Model Layer** serves as the foundation for managing business logic, data processing, and data
persistence. It handles interactions between the application and the underlying data sources, such
as local databases, remote APIs, or in-memory data structures.

### Entities
- [`Model`](../app/src/main/java/com/robifr/ledger/data/model/Model.kt): The base interface for all
  data stored in the [`LocalDatabase`](../app/src/main/java/com/robifr/ledger/local/LocalDatabase.kt),
  excluding Full-Text Search (FTS) related models. It represents the complete structure of an entity
  and is used for operations requiring full data access.
- [`Info`](../app/src/main/java/com/robifr/ledger/data/model/Info.kt): A lightweight version of a
  `Model` that includes only the necessary fields. It's designed to optimize SQL `SELECT` queries by
  reducing overhead when full model details aren't needed.
- [`GithubReleaseModel`](../app/src/main/java/com/robifr/ledger/network/GithubReleaseModel.kt): A
  class that contains information about app updates retrieved from GitHub.
- Data Stored in `SharedPreferences`: User configurations, such as the user's language preference,
  are stored in `SharedPreferences`. The data is accessed using specific keys, which are typically
  defined as constants, like those in the [`SettingsPreferences`](../app/src/main/java/com/robifr/ledger/preferences/SettingsPreferences.kt).
