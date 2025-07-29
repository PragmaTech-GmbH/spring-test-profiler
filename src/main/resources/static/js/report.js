// Spring Test Profiler Report JavaScript Functions

// Initialize empty context statistics (will be populated by Thymeleaf injection)
window.contextStatistics = [];

/**
 * Toggle visibility of test method details
 * @param {HTMLElement} element - The clicked element
 */
function toggleClass(element) {
  const methods = element.nextElementSibling;
  if (methods) {
    methods.classList.toggle('show');
  }
}

/**
 * Toggle the theory section visibility
 */
function toggleTheorySection() {
  const content = document.getElementById('theory-content');
  const icon = document.getElementById('theory-toggle-icon');

  if (!content || !icon) return;

  // Check if content is currently hidden using computed style
  const isHidden = window.getComputedStyle(content).display === 'none';

  if (isHidden) {
    // Content is hidden, so we're opening it - show down arrow
    content.style.display = 'block';
    icon.textContent = '▼';
    icon.classList.add('expanded');
  } else {
    // Content is visible, so we're closing it - show right arrow
    content.style.display = 'none';
    icon.textContent = '▶';
    icon.classList.remove('expanded');
  }
}

/**
 * Test Class Search functionality
 */
class TestClassSearcher {
  constructor() {
    this.contextData = window.contextStatistics || [];
    this.searchInput = null;
    this.suggestionsContainer = null;
    this.resultsContainer = null;
    this.currentSearchTerm = '';
    this.init();
  }

  init() {
    this.searchInput = document.getElementById('test-class-search');
    this.suggestionsContainer = document.getElementById('test-class-suggestions');
    this.resultsContainer = document.getElementById('test-class-search-results');

    if (!this.searchInput || !this.suggestionsContainer || !this.resultsContainer) {
      return;
    }

    this.bindEvents();
  }

  bindEvents() {
    // Input event for real-time search
    this.searchInput.addEventListener('input', (e) => {
      const searchTerm = e.target.value.trim();
      this.currentSearchTerm = searchTerm;

      if (searchTerm.length >= 3) {
        this.performSearch(searchTerm);
        this.showSuggestions(searchTerm);
      } else {
        this.clearResults();
        this.hideSuggestions();
      }
    });

    // Focus events
    this.searchInput.addEventListener('focus', () => {
      if (this.currentSearchTerm.length >= 3) {
        this.showSuggestions(this.currentSearchTerm);
      }
    });

    // Hide suggestions when clicking outside
    document.addEventListener('click', (e) => {
      if (!this.searchInput.contains(e.target) && !this.suggestionsContainer.contains(e.target)) {
        this.hideSuggestions();
      }
    });

    // Keyboard navigation for suggestions
    this.searchInput.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') {
        this.hideSuggestions();
        this.searchInput.blur();
      }
    });
  }

  performSearch(searchTerm) {
    const matches = this.findMatches(searchTerm);
    this.displayResults(matches, searchTerm);
  }

  findMatches(searchTerm) {
    const searchTermLower = searchTerm.toLowerCase();
    const matches = [];

    this.contextData.forEach(context => {
      if (context.testClasses && context.testClasses.length > 0) {
        context.testClasses.forEach(testClass => {
          const simpleClassName = testClass.split('.').pop();
          const fullClassName = testClass;

          // Check if search term matches either full name or simple name
          if (fullClassName.toLowerCase().includes(searchTermLower) ||
              simpleClassName.toLowerCase().includes(searchTermLower)) {
            matches.push({
              testClass: testClass,
              simpleClassName: simpleClassName,
              contextKey: context.contextKey,
              contextBeans: context.numberOfBeans || 0
            });
          }
        });
      }
    });

    // Sort matches: exact simple class name matches first, then alphabetical
    return matches.sort((a, b) => {
      const aSimpleExact = a.simpleClassName.toLowerCase() === searchTermLower;
      const bSimpleExact = b.simpleClassName.toLowerCase() === searchTermLower;

      if (aSimpleExact && !bSimpleExact) return -1;
      if (!aSimpleExact && bSimpleExact) return 1;

      return a.simpleClassName.localeCompare(b.simpleClassName);
    });
  }

  showSuggestions(searchTerm) {
    const matches = this.findMatches(searchTerm);
    const maxSuggestions = 5;

    if (matches.length === 0) {
      this.hideSuggestions();
      return;
    }

    this.suggestionsContainer.innerHTML = '';

    matches.slice(0, maxSuggestions).forEach(match => {
      const suggestionItem = document.createElement('div');
      suggestionItem.className = 'suggestion-item';

      suggestionItem.innerHTML = `
        <span class="suggestion-class-name">${match.simpleClassName}</span>
        <span class="suggestion-context-id">${match.contextKey}</span>
      `;

      suggestionItem.addEventListener('click', () => {
        this.searchInput.value = match.simpleClassName;
        this.currentSearchTerm = match.simpleClassName;
        this.performSearch(match.simpleClassName);
        this.hideSuggestions();
        this.scrollToContext(match.contextKey);
      });

      this.suggestionsContainer.appendChild(suggestionItem);
    });

    this.suggestionsContainer.classList.add('show');
  }

  hideSuggestions() {
    this.suggestionsContainer.classList.remove('show');
  }

  displayResults(matches, searchTerm) {
    if (matches.length === 0) {
      this.resultsContainer.innerHTML = `
        <div class="search-results-header">No test classes found matching "${searchTerm}"</div>
      `;
      this.resultsContainer.classList.add('show');
      return;
    }

    const headerText = matches.length === 1
      ? `Found 1 test class matching "${searchTerm}"`
      : `Found ${matches.length} test classes matching "${searchTerm}"`;

    let resultsHtml = `<div class="search-results-header">${headerText}</div>`;

    matches.forEach(match => {
      resultsHtml += `
        <div class="search-result-item">
          <span class="search-result-class" title="${match.testClass}">${match.simpleClassName}</span>
          <span class="search-result-context" onclick="testClassSearcher.scrollToContext('${match.contextKey}')"
                title="Click to scroll to context entry">${match.contextKey}</span>
        </div>
      `;
    });

    this.resultsContainer.innerHTML = resultsHtml;
    this.resultsContainer.classList.add('show');
  }

  clearResults() {
    this.resultsContainer.classList.remove('show');
    this.resultsContainer.innerHTML = '';
  }

  scrollToContext(contextKey) {
    // Find cache entry with matching context ID
    const cacheEntries = document.querySelectorAll('.cache-entry');

    for (const entry of cacheEntries) {
      const cacheIdElement = entry.querySelector('.cache-id');
      if (cacheIdElement && cacheIdElement.textContent.includes(contextKey.replace('context-', ''))) {
        entry.scrollIntoView({
          behavior: 'smooth',
          block: 'center',
          inline: 'nearest'
        });

        // Highlight the entry briefly
        entry.style.transition = 'background-color 0.3s ease';
        entry.style.backgroundColor = '#e3f2fd';

        setTimeout(() => {
          entry.style.backgroundColor = '';
        }, 2000);

        break;
      }
    }
  }
}

/**
 * Context Comparison Visualizer using D3.js
 */
class ContextComparator {
  constructor() {
    this.contextData = window.contextStatistics || [];
    this.selectedContextA = null;
    this.selectedContextB = null;
    this.selectedAttribute = null;
    this.init();
  }

  init() {
    this.populateDropdowns();
    this.bindEvents();
    this.preselectDefaultContexts();
  }

  populateDropdowns() {
    const contextASelect = document.getElementById('context-a-select');
    const contextBSelect = document.getElementById('context-b-select');

    if (!contextASelect || !contextBSelect) return;

    this.contextData.forEach(context => {
      const optionA = document.createElement('option');
      optionA.value = context.contextKey;
      optionA.textContent = `${context.contextKey} (${context.numberOfBeans} beans, ${context.testClasses.length} classes)`;
      contextASelect.appendChild(optionA);

      const optionB = document.createElement('option');
      optionB.value = context.contextKey;
      optionB.textContent = `${context.contextKey} (${context.numberOfBeans} beans, ${context.testClasses.length} classes)`;
      contextBSelect.appendChild(optionB);
    });
  }

  preselectDefaultContexts() {
    if (this.contextData.length >= 2) {
      const contextASelect = document.getElementById('context-a-select');
      const contextBSelect = document.getElementById('context-b-select');

      if (contextASelect && contextBSelect) {
        // Select first context for A
        contextASelect.value = this.contextData[0].contextKey;
        this.selectedContextA = this.contextData[0];

        // Select second context for B
        contextBSelect.value = this.contextData[1].contextKey;
        this.selectedContextB = this.contextData[1];

        // Update compare button state and trigger comparison
        this.updateCompareButton();
        this.compareContexts();
      }
    }
  }

  bindEvents() {
    const contextASelect = document.getElementById('context-a-select');
    const contextBSelect = document.getElementById('context-b-select');
    const compareBtn = document.getElementById('compare-contexts-btn');

    if (!contextASelect || !contextBSelect || !compareBtn) return;

    contextASelect.addEventListener('change', (e) => {
      if (e.target.value) {
        this.selectedContextA = this.contextData.find(ctx => ctx.contextKey === e.target.value);
      } else {
        this.selectedContextA = null;
      }
      this.updateCompareButton();
    });

    contextBSelect.addEventListener('change', (e) => {
      if (e.target.value) {
        this.selectedContextB = this.contextData.find(ctx => ctx.contextKey === e.target.value);
      } else {
        this.selectedContextB = null;
      }
      this.updateCompareButton();
    });

    compareBtn.addEventListener('click', (e) => {
      e.preventDefault();
      if (!compareBtn.disabled) {
        this.compareContexts();
      }
    });
  }

  updateCompareButton() {
    const compareBtn = document.getElementById('compare-contexts-btn');
    if (!compareBtn) return;

    const canCompare = this.selectedContextA && this.selectedContextB &&
                      this.selectedContextA.contextKey !== this.selectedContextB.contextKey;

    compareBtn.disabled = !canCompare;
  }

  compareContexts() {
    if (!this.selectedContextA || !this.selectedContextB) {
      return;
    }

    const legend = document.getElementById('context-comparison-legend');
    if (legend) {
      legend.style.display = 'flex';
    }

    this.renderComparison();
    this.autoSelectDifferentAttribute();
  }

  renderComparison() {
    const container = d3.select('#context-comparison-visualization');
    container.selectAll('*').remove();

    const width = 1000;
    const height = 600; // Increased height to accommodate bigger visualization
    const contextWidth = 400;
    const contextHeight = 400;

    const svg = container.append('svg')
      .attr('width', width)
      .attr('height', height);

    // Add title
    svg.append('text')
      .attr('x', width / 2)
      .attr('y', 30)
      .attr('text-anchor', 'middle')
      .attr('class', 'comparison-title')
      .style('font-size', '20px')
      .style('font-weight', 'bold')
      .text('Spring Test Context Comparison');

    // Add subtitle
    svg.append('text')
      .attr('x', width / 2)
      .attr('y', 60)
      .attr('text-anchor', 'middle')
      .attr('class', 'comparison-title')
      .style('font-size', '14px')
      .text('Click on any attribute circle (green/red) around the contexts below to see detailed differences.');

    // Context A
    this.renderContext(svg, this.selectedContextA, 50, 100, 'Test Context A');

    // Context B
    this.renderContext(svg, this.selectedContextB, 550, 100, 'Test Context B');
  }

  renderContext(svg, context, x, y, title) {
    const group = svg.append('g').attr('transform', `translate(${x}, ${y})`);

    // Title
    group.append('text')
      .attr('x', 200)
      .attr('y', 0)
      .attr('text-anchor', 'middle')
      .style('font-size', '18px')
      .style('font-weight', 'bold')
      .text(title);

    // Context Key (smaller subtitle)
    group.append('text')
      .attr('x', 200)
      .attr('y', 25)
      .attr('text-anchor', 'middle')
      .style('font-size', '12px')
      .style('font-weight', 'normal')
      .style('fill', '#7f8c8d')
      .text(context.contextKey);

    // Central beans circle
    const centerX = 200;
    const centerY = 240; // Increased from 200 to provide more space below titles
    const centralRadius = 80;

    group.append('circle')
      .attr('cx', centerX)
      .attr('cy', centerY)
      .attr('r', centralRadius)
      .attr('fill', '#3498db')
      .attr('stroke', '#2980b9')
      .attr('stroke-width', 2);

    group.append('text')
      .attr('x', centerX)
      .attr('y', centerY - 10)
      .attr('text-anchor', 'middle')
      .attr('fill', 'white')
      .style('font-size', '16px')
      .style('font-weight', 'bold')
      .text('Beans');

    group.append('text')
      .attr('x', centerX)
      .attr('y', centerY + 10)
      .attr('text-anchor', 'middle')
      .attr('fill', 'white')
      .style('font-size', '18px')
      .style('font-weight', 'bold')
      .text(`(${context.numberOfBeans})`);

    // Configuration features
    const features = this.getContextFeatures(context);
    const angleStep = (2 * Math.PI) / features.length;
    const satelliteRadius = 180;

    features.forEach((feature, i) => {
      const angle = i * angleStep - Math.PI / 2;
      const featureX = centerX + Math.cos(angle) * satelliteRadius;
      const featureY = centerY + Math.sin(angle) * satelliteRadius;

      const featureGroup = group.append('g')
        .attr('transform', `translate(${featureX}, ${featureY})`);

      // Feature circle
      const circle = featureGroup.append('circle')
        .attr('cx', 0)
        .attr('cy', 0)
        .attr('r', 25)
        .attr('fill', this.getFeatureColor(feature))
        .attr('stroke', '#34495e')
        .attr('stroke-width', 1)
        .attr('data-clickable', 'true')
        .style('cursor', 'pointer')
        .on('click', (event) => {
          event.stopPropagation();
          this.handleCircleClick(feature);
        });

      // Add selected class if this is the currently selected attribute
      if (this.selectedAttribute && this.selectedAttribute.key === feature.key) {
        circle.classed('selected', true);
      }

      // Feature label
      featureGroup.append('text')
        .attr('x', 0)
        .attr('y', 35)
        .attr('text-anchor', 'middle')
        .style('font-size', '10px')
        .style('font-weight', 'bold')
        .text(feature.name);

      // Connecting line
      group.append('line')
        .attr('x1', centerX + Math.cos(angle) * (centralRadius + 5))
        .attr('y1', centerY + Math.sin(angle) * (centralRadius + 5))
        .attr('x2', featureX - Math.cos(angle) * 30)
        .attr('y2', featureY - Math.sin(angle) * 30)
        .attr('stroke', '#bdc3c7')
        .attr('stroke-width', 2)
        .attr('stroke-dasharray', '5,5');
    });
  }

  createTestClassesList(testClasses, fontSize = '12px') {
    const list = document.createElement('ul');
    list.style.cssText = `margin: 0; padding-left: 20px; font-family: monospace; font-size: ${fontSize}; line-height: 1.4;`;

    if (testClasses.length === 0) {
      const li = document.createElement('li');
      li.textContent = 'No test classes found';
      li.style.color = '#7f8c8d';
      list.appendChild(li);
    } else {
      testClasses.forEach(testClass => {
        const li = document.createElement('li');
        li.textContent = testClass.split('.').pop(); // Show simple class name
        li.title = testClass; // Full name on hover
        li.style.cssText = 'margin-bottom: 2px; color: #34495e;';
        list.appendChild(li);
      });
    }

    return list;
  }

  createContextDiv(contextLetter, titleFontSize = '16px', listFontSize = '12px') {
    const contextDiv = document.createElement('div');
    contextDiv.style.cssText = 'flex: 1; background: #f8f9fa; padding: 15px; border-radius: 8px; border-left: 4px solid #3498db;';

    const title = document.createElement('h4');
    title.textContent = `Test Context ${contextLetter} - Test Classes`;
    title.style.cssText = `margin: 0 0 10px 0; color: #2c3e50; font-size: ${titleFontSize};`;
    contextDiv.appendChild(title);

    const testClasses = contextLetter === 'A' ? this.selectedContextA.testClasses || [] : this.selectedContextB.testClasses || [];
    const list = this.createTestClassesList(testClasses, listFontSize);
    contextDiv.appendChild(list);

    return contextDiv;
  }

  renderTestClassesLists() {
    const container = document.getElementById('context-comparison-visualization');

    // Create or update test classes container - append to same container as SVG, not parent
    let testClassesContainer = document.getElementById('test-classes-container');
    if (!testClassesContainer) {
      testClassesContainer = document.createElement('div');
      testClassesContainer.id = 'test-classes-container';
      testClassesContainer.style.cssText = 'display: flex; justify-content: space-between; margin-top: 20px; gap: 40px;';
      container.appendChild(testClassesContainer);
    }
    testClassesContainer.innerHTML = '';

    // Create both context divs using the helper method
    const contextADiv = this.createContextDiv('A', '16px', '12px');
    const contextBDiv = this.createContextDiv('B', '16px', '12px');

    testClassesContainer.appendChild(contextADiv);
    testClassesContainer.appendChild(contextBDiv);
  }

  renderTestClassesInDetailedComparison(detailedContainer) {
    // Remove any existing test classes container in detailed comparison
    const existingTestClasses = detailedContainer.querySelector('.test-classes-comparison');
    if (existingTestClasses) {
      existingTestClasses.remove();
    }

    // Create test classes container for detailed comparison
    const testClassesContainer = document.createElement('div');
    testClassesContainer.className = 'test-classes-comparison';
    testClassesContainer.style.cssText = 'margin-top: 20px; display: flex; gap: 20px;';

    // Create both context divs using the helper method with smaller font
    const contextADiv = this.createContextDiv('A', '14px', '11px');
    const contextBDiv = this.createContextDiv('B', '14px', '11px');

    testClassesContainer.appendChild(contextADiv);
    testClassesContainer.appendChild(contextBDiv);

    // Append to the detailed comparison container
    detailedContainer.appendChild(testClassesContainer);
  }

  getContextFeatures(context) {
    const config = context.contextConfiguration;
    return [
      { name: 'Locations', value: config.locations, key: 'locations' },
      { name: 'Property Source Properties', value: config.propertySourceProperties, key: 'propertySourceProperties' },
      { name: 'Classes', value: config.classes, key: 'classes' },
      { name: 'Context Initializer Classes', value: config.contextInitializerClasses, key: 'contextInitializerClasses' },
      { name: 'Active Profiles', value: config.activeProfiles, key: 'activeProfiles' },
      { name: 'Property Source Locations', value: config.propertySourceLocations, key: 'propertySourceLocations' },
      { name: 'Context Customizers', value: config.contextCustomizers, key: 'contextCustomizers' },
      { name: 'Context Loader', value: [config.contextLoader], key: 'contextLoader' },
      { name: 'Parent', value: config.parent ? [config.parent] : [], key: 'parent' }
    ];
  }

  getFeatureColor(feature) {
    if (!this.selectedContextA || !this.selectedContextB) return '#95a5a6';

    const configA = this.selectedContextA.contextConfiguration;
    const configB = this.selectedContextB.contextConfiguration;

    const valueA = this.getFeatureValue(configA, feature.key);
    const valueB = this.getFeatureValue(configB, feature.key);

    const arrayA = Array.isArray(valueA) ? valueA : [valueA];
    const arrayB = Array.isArray(valueB) ? valueB : [valueB];

    if (this.arraysEqual(arrayA, arrayB)) {
      return '#27ae60'; // Same - green
    } else {
      return '#e74c3c'; // Different - red
    }
  }

  getFeatureValue(config, key) {
    switch(key) {
      case 'contextLoader': return config.contextLoader;
      case 'parent': return config.parent;
      default: return config[key] || [];
    }
  }

  arraysEqual(a, b) {
    if (a.length !== b.length) return false;
    const sortedA = [...a].sort();
    const sortedB = [...b].sort();
    return sortedA.every((val, i) => val === sortedB[i]);
  }

  truncateString(str, maxLength = 200) {
    if (!str || str.length <= maxLength) return str;
    return str.substring(0, maxLength) + '...';
  }

  formatValueArray(valueArray, maxLength = 200) {
    if (!valueArray || valueArray.length === 0) return 'None';

    const joinedString = valueArray.join(', ');
    return this.truncateString(joinedString, maxLength);
  }

  autoSelectDifferentAttribute() {
    if (!this.selectedContextA || !this.selectedContextB) return;

    const features = this.getContextFeatures(this.selectedContextA);

    // Find the first different attribute (red circle)
    for (const feature of features) {
      const valueA = this.getFeatureValue(this.selectedContextA.contextConfiguration, feature.key);
      const valueB = this.getFeatureValue(this.selectedContextB.contextConfiguration, feature.key);

      const arrayA = Array.isArray(valueA) ? valueA : [valueA];
      const arrayB = Array.isArray(valueB) ? valueB : [valueB];

      if (!this.arraysEqual(arrayA, arrayB)) {
        // Found a different attribute, select it
        this.handleCircleClick(feature);
        return;
      }
    }

    // If no different attributes found, select the first one
    if (features.length > 0) {
      this.handleCircleClick(features[0]);
    }
  }

  handleCircleClick(feature) {
    // Update selected attribute
    this.selectedAttribute = feature;

    // Update visual selection state
    this.updateCircleSelection();

    // Show detailed comparison
    this.showDetailedComparison(feature);
  }

  updateCircleSelection() {
    // Remove selected class from all circles
    d3.selectAll('.comparison-visualization circle[data-clickable="true"]')
      .classed('selected', false);

    // Add selected class to clicked attribute circles (both contexts)
    if (this.selectedAttribute) {
      const selectedFeatureName = this.selectedAttribute.name;

      // Find and select circles that correspond to the same feature in both contexts
      d3.selectAll('.comparison-visualization g g').each(function() {
        const group = d3.select(this);
        const textElement = group.select('text');
        if (textElement.text() === selectedFeatureName) {
          group.select('circle[data-clickable="true"]').classed('selected', true);
        }
      });
    }
  }

  showDetailedComparison(feature) {
    const detailedContainer = document.getElementById('context-detailed-comparison');
    const contextADetail = detailedContainer.querySelector('.context-a-detail .context-detail-value');
    const contextBDetail = detailedContainer.querySelector('.context-b-detail .context-detail-value');
    const titleElement = detailedContainer.querySelector('.detailed-comparison-title');

    if (!detailedContainer || !contextADetail || !contextBDetail) return;

    // Update title
    titleElement.textContent = `${feature.name} - Detailed Comparison`;

    // Get values for both contexts
    const valueA = this.getFeatureValue(this.selectedContextA.contextConfiguration, feature.key);
    const valueB = this.getFeatureValue(this.selectedContextB.contextConfiguration, feature.key);

    const arrayA = Array.isArray(valueA) ? valueA : [valueA];
    const arrayB = Array.isArray(valueB) ? valueB : [valueB];

    // Format and display values
    contextADetail.innerHTML = this.formatDetailedValue(feature.name, arrayA, arrayB, true);
    contextBDetail.innerHTML = this.formatDetailedValue(feature.name, arrayB, arrayA, false);

    // Add test classes lists to the detailed comparison
    this.renderTestClassesInDetailedComparison(detailedContainer);

    // Show the detailed comparison container
    detailedContainer.style.display = 'block';
  }

  formatDetailedValue(attributeName, currentValues, comparisonValues, isContextA) {
    const contextName = isContextA ? 'Context A' : 'Context B';
    const areEqual = this.arraysEqual(currentValues, comparisonValues);

    let html = `<div class="attribute-name">${attributeName}</div>`;

    if (currentValues.length === 0 || (currentValues.length === 1 && !currentValues[0])) {
      html += `<div style="color: #6c757d; font-style: italic; text-align: center; padding: 20px;">No values configured</div>`;
    } else {
      html += `<pre>`;
      currentValues.forEach((value, index) => {
        if (value) {
          const highlightClass = areEqual ? 'highlight-same' : 'highlight-different';
          // Format long strings with line breaks for better readability
          const formattedValue = this.formatLongValue(value, attributeName);
          html += `<span class="${highlightClass}">${this.escapeHtml(formattedValue)}</span>`;
          if (index < currentValues.length - 1) {
            html += '\n';
          }
        }
      });
      html += `</pre>`;

      // Add comparison status
      const statusIcon = areEqual ? '✓' : '✗';
      const statusColor = areEqual ? '#27ae60' : '#e74c3c';
      const statusText = areEqual ? 'Same as other context' : 'Different from other context';
      html += `<div style="margin-top: 15px; padding: 10px; background: ${areEqual ? '#d4edda' : '#f8d7da'}; border-radius: 6px; border-left: 4px solid ${statusColor};">
                 <span style="color: ${statusColor}; font-weight: bold;">${statusIcon} ${statusText}</span>
               </div>`;
    }

    return html;
  }

  formatLongValue(value, attributeName) {
    // For Context Customizers and other very long class names, add line breaks at package boundaries
    if (attributeName === 'Context Customizers' && value.length > 80) {
      // Insert line breaks before common package prefixes for better readability
      return value.replace(/(org\.springframework\.|org\.junit\.|com\.)/g, '\n$1')
                  .replace(/^[\s\n]+/, ''); // Remove leading whitespace/newlines
    }

    // For other long values, wrap at 120 characters maintaining word boundaries
    if (value.length > 120) {
      return value.replace(/(.{120})/g, '$1\n');
    }

    return value;
  }

  escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }
}

/**
 * Initialize the report functionality when DOM is loaded
 */
function initializeReport() {
  // Ensure JSON is parsed first
  try {
    const jsonScript = document.getElementById('context-statistics-json');
    if (jsonScript) {
      window.contextStatistics = JSON.parse(jsonScript.textContent || '[]');
    }
  } catch (e) {
    console.error('Failed to parse context statistics JSON:', e);
    window.contextStatistics = [];
  }

  // Initialize test class searcher
  if (window.contextStatistics && window.contextStatistics.length > 0) {
    window.testClassSearcher = new TestClassSearcher();
    new ContextComparator();
  }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', initializeReport);

// Export functions for testing
if (typeof module !== 'undefined' && module.exports) {
  module.exports = {
    toggleClass,
    toggleTheorySection,
    TestClassSearcher,
    ContextComparator,
    initializeReport
  };
}
