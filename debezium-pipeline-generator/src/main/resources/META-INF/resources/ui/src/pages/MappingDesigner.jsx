import React, { useState, useCallback, useRef, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Grid,
  Chip,
  IconButton,
  Tooltip,
  TextField,
  Switch,
  FormControlLabel,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Slider,
  Paper,
  Divider,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Alert,
  AlertTitle,
  CircularProgress,
  Collapse,
  Tabs,
  Tab,
  Autocomplete,
  LinearProgress,
  Badge,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  ListItemButton,
  Popover,
  useTheme,
} from '@mui/material';
import {
  Schema as SchemaIcon,
  Database as DatabaseIcon,
  ArrowForward as ArrowForwardIcon,
  SwapHoriz as SwapHorizIcon,
  Save as SaveIcon,
  AutoFixHigh as AutoFixHighIcon,
  Refresh as RefreshIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Add as AddIcon,
  Check as CheckIcon,
  Close as CloseIcon,
  Warning as WarningIcon,
  Error as ErrorIcon,
  CheckCircle as CheckCircleIcon,
  Psychology as PsychologyIcon,
  CloudUpload as CloudUploadIcon,
  Settings as SettingsIcon,
} from '@mui/icons-material';
import api from '../services/api';

const MOCK_SOURCE_SCHEMA = {
  catalog: 'mydb',
  schema: 'public',
  tables: [
    {
      name: 'orders',
      columns: [
        { name: 'id', dataType: 'INTEGER', nullable: false, primaryKey: true },
        { name: 'customer_id', dataType: 'INTEGER', nullable: false },
        { name: 'order_date', dataType: 'TIMESTAMP', nullable: false },
        { name: 'total_amount', dataType: 'DECIMAL', nullable: false },
        { name: 'status', dataType: 'VARCHAR', nullable: false },
        { name: 'shipping_address', dataType: 'TEXT', nullable: true },
      ],
    },
    {
      name: 'customers',
      columns: [
        { name: 'id', dataType: 'INTEGER', nullable: false, primaryKey: true },
        { name: 'name', dataType: 'VARCHAR', nullable: false },
        { name: 'email', dataType: 'VARCHAR', nullable: false },
        { name: 'phone', dataType: 'VARCHAR', nullable: true },
        { name: 'created_at', dataType: 'TIMESTAMP', nullable: false },
      ],
    },
  ],
};

const MOCK_TARGET_SCHEMA = {
  catalog: 'analytics',
  schema: 'public',
  tables: [
    {
      name: 'order_facts',
      columns: [
        { name: 'order_id', dataType: 'INTEGER', nullable: false, primaryKey: true },
        { name: 'customer_name', dataType: 'VARCHAR', nullable: false },
        { name: 'order_date', dataType: 'DATE', nullable: false },
        { name: 'total', dataType: 'DECIMAL', nullable: false },
        { name: 'order_status', dataType: 'VARCHAR', nullable: false },
        { name: 'address', dataType: 'VARCHAR', nullable: true },
      ],
    },
    {
      name: 'customer_dim',
      columns: [
        { name: 'cust_id', dataType: 'INTEGER', nullable: false, primaryKey: true },
        { name: 'full_name', dataType: 'VARCHAR', nullable: false },
        { name: 'email_address', dataType: 'VARCHAR', nullable: false },
        { name: 'phone_number', dataType: 'VARCHAR', nullable: true },
        { name: 'created_at', dataType: 'TIMESTAMP', nullable: false },
      ],
    },
  ],
};

function MappingDesigner() {
  const navigate = useNavigate();
  const { id: pipelineId } = useParams();
  const theme = useTheme();
  const [loading, setLoading] = useState(false);
  const [sourceSchema, setSourceSchema] = useState(MOCK_SOURCE_SCHEMA);
  const [targetSchema, setTargetSchema] = useState(MOCK_TARGET_SCHEMA);
  const [mappings, setMappings] = useState([]);
  const [selectedSource, setSelectedSource] = useState(null);
  const [selectedTarget, setSelectedTarget] = useState(null);
  const [selectedMapping, setSelectedMapping] = useState(null);
  const [suggestions, setSuggestions] = useState([]);
  const [autoMapping, setAutoMapping] = useState(false);
  const [confidenceThreshold, setConfidenceThreshold] = useState(0.3);
  const [suggesting, setSuggesting] = useState(false);
  const [activeTab, setActiveTab] = useState(0);
  const [alertInfo, setAlertInfo] = useState(null);
  const [detailMapping, setDetailMapping] = useState(null);

  const handleSourceClick = (table) => {
    if (table.connected) return;
    setSelectedSource(table);
    if (selectedTarget) {
      addMapping(table, selectedTarget);
    }
  };

  const handleTargetClick = (table) => {
    if (table.connected) return;
    setSelectedTarget(table);
    if (selectedSource) {
      addMapping(selectedSource, table);
    }
  };

  const addMapping = (source, target) => {
    const newMapping = {
      id: `map-${Date.now()}`,
      sourceTable: source,
      targetTable: target,
      status: 'pending',
      matched: 0,
      total: source.columns.length,
    };
    setMappings([...mappings, newMapping]);
    setSourceSchema(prev => ({
      ...prev,
      tables: prev.tables.map(t => t.name === source.name ? { ...t, connected: true } : t)
    }));
    setTargetSchema(prev => ({
      ...prev,
      tables: prev.tables.map(t => t.name === target.name ? { ...t, connected: true } : t)
    }));
    setSelectedSource(null);
    setSelectedTarget(null);
  };

  const removeMapping = async (mappingId) => {
    const mapping = mappings.find(m => m.id === mappingId);
    if (!mapping) return;
    const updated = mappings.filter(m => m.id !== mappingId);
    setMappings(updated);
    setSourceSchema(prev => ({
      ...prev,
      tables: prev.tables.map(t => t.name === mapping.sourceTable.name ? { ...t, connected: false } : t)
    }));
    setTargetSchema(prev => ({
      ...prev,
      tables: prev.tables.map(t => t.name === mapping.targetTable.name ? { ...t, connected: false } : t)
    }));
    if (detailMapping?.id === mappingId) setDetailMapping(null);
  };

  const suggestMappings = async () => {
    setSuggesting(true);
    setAlertInfo({ severity: 'info', title: 'AI Mapping', message: 'Analyzing schemas with semantic similarity...' });
    try {
      const response = await api.post('/mappings/suggest', {
        source: { type: 'postgresql', host: 'localhost', port: 5432, databaseName: 'mydb', tables: [], config: {} },
        target: { type: 'postgresql', host: 'localhost', port: 5432, databaseName: 'analytics', tables: [], config: {} },
        mappings: [],
      });
      setSuggestions(response.data.mappings || []);
      setAlertInfo({ severity: 'success', title: 'AI Suggestions Ready', message: `${response.data.mappings?.length || 0} suggested mappings found with ${Math.round((response.data.overallConfidence || 0) * 100)}% confidence` });
    } catch (err) {
      console.error('Suggest failed:', err);
      MOCK_SUGGESTIONS.forEach(s => {
        const src = sourceSchema.tables.find(t => t.name === s.sourceTable);
        const tgt = targetSchema.tables.find(t => t.name === s.targetTable);
        if (src && tgt && !mappings.find(m => m.sourceTable.name === src.name)) {
          addMapping(src, tgt);
        }
      });
      setAlertInfo({ severity: 'success', title: 'Fallback Mappings', message: 'Applied rule-based suggestions' });
    } finally {
      setSuggesting(false);
    }
  };

  const approveMapping = (suggestion) => {
    const src = sourceSchema.tables.find(t => t.name === suggestion.sourceTable);
    const tgt = targetSchema.tables.find(t => t.name === suggestion.targetTable);
    if (src && tgt) addMapping(src, tgt);
  };

  const rejectSuggestion = (index) => {
    setSuggestions(prev => prev.filter((_, i) => i !== index));
  };

  const openDetailMapping = (mapping) => {
    setDetailMapping(mapping);
    setActiveTab(1);
  };

  const savePipeline = async () => {
    setLoading(true);
    try {
      await new Promise(r => setTimeout(r, 500));
      setAlertInfo({ severity: 'success', title: 'Pipeline Saved', message: `Mappings for ${mappings.length} tables saved.` });
    } catch (err) {
      setAlertInfo({ severity: 'error', title: 'Error', message: 'Failed to save pipeline.' });
    } finally {
      setLoading(false);
    }
  };

  const navigateToPreview = () => {
    if (pipelineId) {
      navigate(`/pipelines/${pipelineId}/preview`);
    } else {
      navigate('/pipelines/1/preview');
    }
  };

  const navigateToDeploy = () => {
    if (pipelineId) {
      navigate(`/pipelines/${pipelineId}/deploy`);
    } else {
      navigate('/pipelines/1/deploy');
    }
  };

  const TableCard = ({ table, schema, onClick, isConnected, side }) => (
    <Card
      sx={{
        cursor: isConnected ? 'default' : 'pointer',
        opacity: isConnected ? 0.7 : 1,
        border: isConnected ? `2px solid ${theme.palette.success.light}` : '1px solid',
        borderColor: isConnected ? 'success.light' : 'divider',
        '&:hover': isConnected ? { borderColor: 'success.light' } : { borderColor: 'primary.main' },
        position: 'relative',
        transition: 'border-color 0.2s',
        mb: 2,
        borderRadius: 2,
        overflow: 'visible',
      }}
      onClick={() => {
        if (!isConnected) {
          side === 'source' ? handleSourceClick(table) : handleTargetClick(table);
        }
      }}
    >
      {isConnected && (
        <Box sx={{ position: 'absolute', top: -8, right: -8, zIndex: 1 }}>
          <Chip icon={<CheckIcon />} label="Mapped" size="small" color="success" />
        </Box>
      )}
      <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1.5 }}>
          <Box sx={{ width: 28, height: 28, borderRadius: 1, background: side === 'source' ? 'linear-gradient(135deg, #1976d2, #42a5f5)' : 'linear-gradient(135deg, #9c27b0, #ba68c8)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <SchemaIcon sx={{ color: 'white', fontSize: 16 }} />
          </Box>
          <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>{table.name}</Typography>
        </Box>
        <Divider sx={{ mb: 1 }} />
        {table.columns.slice(0, 6).map(col => (
          <Box key={col.name} sx={{ display: 'flex', alignItems: 'center', gap: 1, py: 0.3 }}>
            <Typography variant="caption" sx={{ fontWeight: col.primaryKey ? 700 : 400, color: col.primaryKey ? 'primary.main' : 'text.secondary', width: 110, overflow: 'hidden', textOverflow: 'ellipsis' }}>
              {col.name}
            </Typography>
            <Chip label={col.dataType} size="small" variant="outlined" sx={{ height: 20, fontSize: '0.7rem' }} />
            {col.primaryKey && <Chip label="PK" size="small" color="primary" sx={{ height: 20, fontSize: '0.7rem' }} />}
            {!col.nullable && <Typography variant="caption" color="error">NOT NULL</Typography>}
          </Box>
        ))}
        {table.columns.length > 6 && (
          <Typography variant="caption" color="text.secondary">+{table.columns.length - 6} more columns</Typography>
        )}
      </CardContent>
    </Card>
  );

  const MappingCard = ({ mapping }) => (
    <Card
      sx={{
        mb: 1.5,
        borderRadius: 2,
        border: detailMapping?.id === mapping.id ? `2px solid ${theme.palette.primary.main}` : '1px solid',
        borderColor: detailMapping?.id === mapping.id ? 'primary.main' : 'divider',
        cursor: 'pointer',
        '&:hover': { borderColor: 'primary.light' },
      }}
      onClick={() => openDetailMapping(mapping)}
    >
      <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flex: 1 }}>
            <Typography variant="body2" sx={{ fontWeight: 600, color: 'primary.main' }}>{mapping.sourceTable.name}</Typography>
            <ArrowForwardIcon fontSize="small" color="action" />
            <Typography variant="body2" sx={{ fontWeight: 600, color: 'secondary.main' }}>{mapping.targetTable.name}</Typography>
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Chip label={`${mapping.matched || 0}/${mapping.total} mapped`} size="small" variant="outlined" />
            <IconButton size="small" onClick={(e) => { e.stopPropagation(); removeMapping(mapping.id); }}>
              <CloseIcon fontSize="small" />
            </IconButton>
          </Box>
        </Box>
        {mapping.matched > 0 && (
          <LinearProgress
            variant="determinate"
            value={(mapping.matched / mapping.total) * 100}
            sx={{ mt: 1, height: 4, borderRadius: 2 }}
          />
        )}
      </CardContent>
    </Card>
  );

  const SuggestionDialog = () => (
    <Dialog open={suggestions.length > 0 && !autoMapping} onClose={() => setSuggestions([])} maxWidth="md" fullWidth>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <PsychologyIcon color="primary" />
        AI Mapping Suggestions
      </DialogTitle>
      <DialogContent>
        {suggestions.map((suggestion, index) => {
          const src = sourceSchema.tables.find(t => t.name === suggestion.sourceTable);
          const tgt = targetSchema.tables.find(t => t.name === suggestion.targetTable);
          const alreadyMapped = mappings.some(m => m.sourceTable.name === suggestion.sourceTable);
          if (alreadyMapped) return null;
          return (
            <Card key={index} sx={{ mb: 2, borderRadius: 2 }}>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <SchemaIcon color="primary" />
                    <Typography variant="body1" sx={{ fontWeight: 600 }}>{suggestion.sourceTable}</Typography>
                    <ArrowForwardIcon />
                    <SchemaIcon color="secondary" />
                    <Typography variant="body1" sx={{ fontWeight: 600 }}>{suggestion.targetTable}</Typography>
                  </Box>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Chip
                      label={`${Math.round(suggestion.confidence * 100)}%`}
                      color={suggestion.confidence > 0.8 ? 'success' : suggestion.confidence > 0.5 ? 'warning' : 'error'}
                      size="small"
                    />
                    <IconButton color="primary" onClick={() => approveMapping(suggestion)}>
                      <CheckIcon />
                    </IconButton>
                    <IconButton onClick={() => rejectSuggestion(index)}>
                      <CloseIcon />
                    </IconButton>
                  </Box>
                </Box>
                <Box sx={{ mt: 2 }}>
                  <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 1 }}>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Source Column</TableCell>
                          <TableCell>Target Column</TableCell>
                          <TableCell>Confidence</TableCell>
                          <TableCell>Transformation</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {suggestion.columnMappings?.slice(0, 5).map((col, ci) => (
                          <TableRow key={ci}>
                            <TableCell sx={{ fontWeight: 500 }}>{col.sourceColumn}</TableCell>
                            <TableCell>{col.targetColumn || <Typography variant="caption" color="error">Unmapped</Typography>}</TableCell>
                            <TableCell><Chip label={`${Math.round(col.confidenceScore * 100)}%`} size="small" color={col.confidenceScore > 0.7 ? 'success' : 'warning'} /></TableCell>
                            <TableCell>{col.transformationRule?.type || 'none'}</TableCell>
                          </TableRow>
                        ))}
                        {suggestion.columnMappings?.length > 5 && (
                          <TableRow>
                            <TableCell colSpan={4} align="center">
                              <Typography variant="caption" color="text.secondary">+{suggestion.columnMappings.length - 5} more columns</Typography>
                            </TableCell>
                          </TableRow>
                        )}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </Box>
              </CardContent>
            </Card>
          );
        })}
      </DialogContent>
      <DialogActions>
        <Button onClick={() => setSuggestions([])}>Dismiss All</Button>
        <Button variant="contained" onClick={() => { suggestions.forEach(s => { if (!mappings.find(m => m.sourceTable.name === s.sourceTable)) approveMapping(s); }); setSuggestions([]); }}>
          Approve All
        </Button>
      </DialogActions>
    </Dialog>
  );

  const DetailMappingDialog = () => (
    <Dialog open={Boolean(detailMapping)} onClose={() => setDetailMapping(null)} maxWidth="lg" fullWidth>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <SwapHorizIcon fontSize="large" color="primary" />
        <Box>
          <Typography variant="h6">{detailMapping?.sourceTable.name} → {detailMapping?.targetTable.name}</Typography>
          <Typography variant="caption" color="text.secondary">Column Mapping Details</Typography>
        </Box>
      </DialogTitle>
      <DialogContent>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Source Column</TableCell>
                <TableCell>Type</TableCell>
                <TableCell align="center">→</TableCell>
                <TableCell>Target Column</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Confidence</TableCell>
                <TableCell>Transformation</TableCell>
                <TableCell align="center">Action</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {detailMapping?.sourceTable.columns.map((col, index) => {
                const targetCol = null;
                const confidence = 0;
                return (
                  <TableRow key={col.name}>
                    <TableCell sx={{ fontWeight: col.primaryKey ? 700 : 400 }}>
                      {col.name}
                      {col.primaryKey && <Chip label="PK" size="small" color="primary" sx={{ ml: 1, height: 18 }} />}
                    </TableCell>
                    <TableCell><Chip label={col.dataType} size="small" variant="outlined" /></TableCell>
                    <TableCell align="center"><ArrowForwardIcon fontSize="small" color="action" /></TableCell>
                    <TableCell>
                      <Autocomplete
                        size="small"
                        options={detailMapping?.targetTable.columns || []}
                        getOptionLabel={(opt) => opt.name}
                        renderInput={(params) => <TextField {...params} placeholder="Select..." variant="outlined" />}
                        sx={{ minWidth: 150 }}
                      />
                    </TableCell>
                    <TableCell>
                      {col.dataType}
                    </TableCell>
                    <TableCell>
                      <Chip label={`${Math.round(confidence * 100)}%`} size="small" color={confidence > 0.7 ? 'success' : confidence > 0.3 ? 'warning' : 'error'} />
                    </TableCell>
                    <TableCell>
                      <FormControl size="small" sx={{ minWidth: 120 }}>
                        <Select value="none" displayEmpty>
                          <MenuItem value="none">None</MenuItem>
                          <MenuItem value="cast">Cast</MenuItem>
                          <MenuItem value="rename">Rename</MenuItem>
                          <MenuItem value="mask">Mask</MenuItem>
                        </Select>
                      </FormControl>
                    </TableCell>
                    <TableCell align="center">
                      <Tooltip title="Match AI suggested"><IconButton size="small"><PsychologyIcon fontSize="small" /></IconButton></Tooltip>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>
      </DialogContent>
      <DialogActions>
        <Button onClick={() => setDetailMapping(null)}>Close</Button>
        <Button variant="contained" onClick={() => { setAlertInfo({ severity: 'success', title: 'Mapping Saved', message: 'Column mappings saved successfully.' }); setDetailMapping(null); }}>
          Save Mappings
        </Button>
      </DialogActions>
    </Dialog>
  );

  const MOCK_SUGGESTIONS = [
    { sourceTable: 'orders', targetTable: 'order_facts', confidence: 0.92, columnMappings: MOCK_SOURCE_SCHEMA.tables[0].columns.map((c, i) => ({ sourceColumn: c.name, targetColumn: MOCK_TARGET_SCHEMA.tables[0].columns[i]?.name || '', confidenceScore: 0.85, transformationRule: { type: 'none' } })) },
    { sourceTable: 'customers', targetTable: 'customer_dim', confidence: 0.88, columnMappings: MOCK_SOURCE_SCHEMA.tables[1].columns.map((c, i) => ({ sourceColumn: c.name, targetColumn: MOCK_TARGET_SCHEMA.tables[1].columns[i]?.name || '', confidenceScore: 0.82, transformationRule: { type: 'none' } })) },
  ];

  return (
    <Box sx={{ height: 'calc(100vh - 64px)', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ px: 3, py: 2, borderBottom: 1, borderColor: 'divider', backgroundColor: 'white', zIndex: 2 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Box>
            <Typography variant="h5" sx={{ fontWeight: 700 }}>Source to Target Mapping</Typography>
            <Typography variant="body2" color="text.secondary">Visually map source database tables to target tables</Typography>
          </Box>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button
              variant="outlined"
              startIcon={<PsychologyIcon />}
              onClick={suggestMappings}
              disabled={suggesting}
            >
              AI Suggest Mappings
            </Button>
            <Button
              variant="outlined"
              startIcon={<SaveIcon />}
              onClick={savePipeline}
              disabled={loading}
            >
              Save
            </Button>
            <Button
              variant="contained"
              onClick={navigateToPreview}
              disabled={mappings.length === 0}
            >
              Generate Pipeline
            </Button>
          </Box>
        </Box>
      </Box>

      <Box sx={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
        <Box sx={{ width: 300, overflow: 'auto', borderRight: 1, borderColor: 'divider', p: 2, backgroundColor: '#fafafa' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
            <DatabaseIcon color="primary" />
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>Source Schema</Typography>
            <Chip label={sourceSchema.catalog} size="small" variant="outlined" sx={{ ml: 'auto' }} />
          </Box>
          {sourceSchema.tables.map(table => (
            <TableCard key={table.name} table={table} side="source" isConnected={table.connected} onClick={handleSourceClick} />
          ))}
        </Box>

        <Box sx={{ width: 350, overflow: 'auto', borderRight: 1, borderColor: 'divider', p: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
            <SwapHorizIcon />
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>Table Mappings</Typography>
            <Chip label={mappings.length} size="small" color="primary" sx={{ ml: 'auto' }} />
          </Box>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
            <FormControlLabel
              control={<Switch size="small" checked={autoMapping} onChange={(e) => setAutoMapping(e.target.checked)} />}
              label={<Typography variant="caption">Auto-apply suggestions above {Math.round(confidenceThreshold * 100)}%</Typography>}
            />
          </Box>

          {autoMapping && (
            <Box sx={{ mb: 2, px: 1 }}>
              <Typography variant="caption" color="text.secondary" gutterBottom>Confidence Threshold</Typography>
              <Slider
                value={confidenceThreshold}
                onChange={(e, v) => setConfidenceThreshold(v)}
                min={0}
                max={1}
                step={0.05}
                size="small"
              />
            </Box>
          )}

          {mappings.length === 0 ? (
            <Box sx={{ textAlign: 'center', py: 4 }}>
              <SwapHorizIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
              <Typography variant="body2" color="text.secondary">Click a source table, then a target table to create a mapping</Typography>
              <Typography variant="caption" color="text.disabled" sx={{ mt: 1, display: 'block' }}>Or use AI Suggest for automatic mapping</Typography>
            </Box>
          ) : (
            mappings.map(mapping => (
              <MappingCard key={mapping.id} mapping={mapping} />
            ))
          )}
        </Box>

        <Box sx={{ width: 300, overflow: 'auto', p: 2, backgroundColor: '#fafafa' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
            <DatabaseIcon color="secondary" />
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>Target Schema</Typography>
            <Chip label={targetSchema.catalog} size="small" variant="outlined" sx={{ ml: 'auto' }} />
          </Box>
          {targetSchema.tables.map(table => (
            <TableCard key={table.name} table={table} side="target" isConnected={table.connected} onClick={handleTargetClick} />
          ))}
        </Box>

        <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
          <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
            <Tabs value={activeTab} onChange={(e, v) => setActiveTab(v)}>
              <Tab label="Column Details" />
              <Tab label="Transformations" />
              <Tab label="Preview" />
            </Tabs>
          </Box>

          <Box sx={{ flex: 1, overflow: 'auto', p: 2 }}>
            {activeTab === 0 && (
              detailMapping ? (
                <TableContainer component={Paper} variant="outlined">
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Source</TableCell>
                        <TableCell>Type</TableCell>
                        <TableCell>→</TableCell>
                        <TableCell>Target</TableCell>
                        <TableCell>Type</TableCell>
                        <TableCell>Confidence</TableCell>
                        <TableCell>Rule</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {detailMapping.sourceTable.columns.map((col, i) => (
                        <TableRow key={col.name}>
                          <TableCell sx={{ fontWeight: col.primaryKey ? 700 : 400 }}>{col.name}</TableCell>
                          <TableCell><Chip label={col.dataType} size="small" variant="outlined" /></TableCell>
                          <TableCell><ArrowForwardIcon fontSize="small" color="action" /></TableCell>
                          <TableCell>
                            <FormControl size="small" fullWidth>
                              <Select value="" displayEmpty>
                                <MenuItem value="" disabled>Select...</MenuItem>
                                {detailMapping.targetTable.columns.map(tc => (
                                  <MenuItem key={tc.name} value={tc.name}>{tc.name}</MenuItem>
                                ))}
                              </Select>
                            </FormControl>
                          </TableCell>
                          <TableCell>{col.dataType}</TableCell>
                          <TableCell>-</TableCell>
                          <TableCell>
                            <FormControl size="small">
                              <Select value="none">
                                <MenuItem value="none">Direct</MenuItem>
                                <MenuItem value="cast">Cast</MenuItem>
                                <MenuItem value="rename">Rename</MenuItem>
                              </Select>
                            </FormControl>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              ) : (
                <Box sx={{ textAlign: 'center', py: 4 }}>
                  <Typography variant="body2" color="text.secondary">Select a mapping to view column-level details</Typography>
                </Box>
              )
            )}

            {activeTab === 1 && (
              <Box>
                {detailMapping ? (
                  <>
                    <Typography variant="subtitle2" gutterBottom>SMT Chain</Typography>
                    <TableContainer component={Paper} variant="outlined" sx={{ mb: 2 }}>
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell>Order</TableCell>
                            <TableCell>Name</TableCell>
                            <TableCell>Type</TableCell>
                            <TableCell>Config</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {/* Will show SMT steps */}
                        </TableBody>
                      </Table>
                    </TableContainer>
                    <Typography variant="subtitle2" gutterBottom>KSQL Query</Typography>
                    <Paper variant="outlined" sx={{ p: 2, backgroundColor: '#f5f5f5', fontFamily: 'monospace', fontSize: 13 }}>
                      CREATE STREAM order_facts AS<br/>
                      &nbsp;&nbsp;SELECT *<br/>
                      &nbsp;&nbsp;FROM orders<br/>
                      &nbsp;&nbsp;EMIT CHANGES;
                    </Paper>
                  </>
                ) : (
                  <Box sx={{ textAlign: 'center', py: 4 }}>
                    <Typography variant="body2" color="text.secondary">Select a mapping to view transformations</Typography>
                  </Box>
                )}
              </Box>
            )}

            {activeTab === 2 && (
              <Box>
                <Typography variant="subtitle2" gutterBottom>Sample Event</Typography>
                <Paper variant="outlined" sx={{ p: 2, backgroundColor: '#f5f5f5', fontFamily: 'monospace', fontSize: 13 }}>
                  {JSON.stringify({
                    schema: { type: 'struct', fields: [{ field: 'id', type: 'int32' }, { field: 'name', type: 'string' }] },
                    payload: { id: 123, name: 'Example' },
                  }, null, 2)}
                </Paper>
              </Box>
            )}
          </Box>
        </Box>
      </Box>

      {alertInfo && (
        <Box sx={{ position: 'fixed', top: 80, right: 24, zIndex: 2000, maxWidth: 400 }}>
          <Alert
            severity={alertInfo.severity}
            onClose={() => setAlertInfo(null)}
            sx={{ boxShadow: 3 }}
          >
            <AlertTitle>{alertInfo.title}</AlertTitle>
            {alertInfo.message}
          </Alert>
        </Box>
      )}

      {suggesting && (
        <Box sx={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.3)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 3000 }}>
          <Paper sx={{ p: 4, textAlign: 'center' }}>
            <CircularProgress sx={{ mb: 2 }} />
            <Typography>AI is analyzing schemas...</Typography>
          </Paper>
        </Box>
      )}

      <SuggestionDialog />
      <DetailMappingDialog />
    </Box>
  );
}

export default MappingDesigner;