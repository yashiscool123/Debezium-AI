import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Grid,
  Card,
  CardContent,
  CardActions,
  Typography,
  Button,
  Chip,
  Box,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Tooltip,
  CircularProgress,
  Alert,
  Tabs,
  Tab,
  TextField,
  InputAdornment,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Visibility as VisibilityIcon,
  PlayArrow as PlayArrowIcon,
  CloudUpload as CloudUploadIcon,
  Settings as SettingsIcon,
  Search as SearchIcon,
  FilterList as FilterListIcon,
  Database as DatabaseIcon,
  Schema as SchemaIcon,
  Sync as SyncIcon,
} from '@mui/icons-material';
import api from '../services/api';

const mockPipelines = [
  {
    id: '1',
    name: 'MySQL to Kafka',
    description: 'Capture changes from MySQL orders database',
    status: 'DEPLOYED',
    sourceType: 'MYSQL',
    targetType: 'KAFKA',
    createdAt: '2024-01-15T10:30:00Z',
    updatedAt: '2024-01-20T14:22:00Z',
    tables: 12,
    throughput: '5.2K events/sec',
  },
  {
    id: '2',
    name: 'PostgreSQL to PostgreSQL',
    description: 'Replicate PostgreSQL inventory to analytics DB',
    status: 'DRAFT',
    sourceType: 'POSTGRESQL',
    targetType: 'POSTGRESQL',
    createdAt: '2024-01-18T09:15:00Z',
    updatedAt: '2024-01-18T09:15:00Z',
    tables: 8,
    throughput: 'N/A',
  },
  {
    id: '3',
    name: 'MongoDB to Kafka',
    description: 'Stream MongoDB product catalog changes',
    status: 'VALIDATING',
    sourceType: 'MONGODB',
    targetType: 'KAFKA',
    createdAt: '2024-01-20T16:45:00Z',
    updatedAt: '2024-01-21T08:30:00Z',
    tables: 5,
    throughput: 'N/A',
  },
];

function Dashboard() {
  const [pipelines, setPipelines] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [tab, setTab] = useState(0);

  useEffect(() => {
    loadPipelines();
  }, []);

  const loadPipelines = async () => {
    setLoading(true);
    try {
      // const response = await api.get('/pipelines');
      // setPipelines(response.data);
      setPipelines(mockPipelines);
    } catch (error) {
      console.error('Failed to load pipelines:', error);
    } finally {
      setLoading(false);
    }
  };

  const filteredPipelines = pipelines.filter(p => {
    const matchesSearch = p.name.toLowerCase().includes(search.toLowerCase()) ||
      p.description.toLowerCase().includes(search.toLowerCase());
    const matchesStatus = statusFilter === 'all' || p.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const getStatusColor = (status) => {
    switch (status) {
      case 'DEPLOYED': return 'success';
      case 'DRAFT': return 'default';
      case 'VALIDATING': return 'warning';
      case 'VALID': return 'info';
      case 'INVALID': return 'error';
      case 'DEPLOYING': return 'warning';
      case 'FAILED': return 'error';
      default: return 'default';
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'DEPLOYED': return <PlayArrowIcon fontSize="small" color="success" />;
      case 'DRAFT': return <SettingsIcon fontSize="small" />;
      case 'VALIDATING': return <SyncIcon fontSize="small" color="warning" />;
      case 'VALID': return <SchemaIcon fontSize="small" color="info" />;
      case 'INVALID': return <SettingsIcon fontSize="small" color="error" />;
      default: return <SettingsIcon fontSize="small" />;
    }
  };

  return (
    <Box sx={{ maxWidth: '1400px', mx: 'auto' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 700 }}>Pipelines</Typography>
          <Typography variant="body1" color="text.secondary">Manage your Debezium CDC pipelines</Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          component={Link}
          to="/pipelines/new"
          size="large"
        >
          Create Pipeline
        </Button>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={3}>
            <Grid item xs={12} sm={6} md={3}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Box sx={{ width: 48, height: 48, borderRadius: 2, background: 'primary.main', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white' }}>
                  <DatabaseIcon fontSize="large" />
                </Box>
                <Box>
                  <Typography variant="h4" sx={{ fontWeight: 700 }}>{pipelines.length}</Typography>
                  <Typography variant="body2" color="text.secondary">Total Pipelines</Typography>
                </Box>
              </Box>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Box sx={{ width: 48, height: 48, borderRadius: 2, background: 'success.main', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white' }}>
                  <PlayArrowIcon fontSize="large" />
                </Box>
                <Box>
                  <Typography variant="h4" sx={{ fontWeight: 700 }}>
                    {pipelines.filter(p => p.status === 'DEPLOYED').length}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">Deployed</Typography>
                </Box>
              </Box>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Box sx={{ width: 48, height: 48, borderRadius: 2, background: 'warning.main', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white' }}>
                  <SyncIcon fontSize="large" />
                </Box>
                <Box>
                  <Typography variant="h4" sx={{ fontWeight: 700 }}>
                    {pipelines.filter(p => p.status === 'VALIDATING' || p.status === 'DEPLOYING').length}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">In Progress</Typography>
                </Box>
              </Box>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Box sx={{ width: 48, height: 48, borderRadius: 2, background: 'error.main', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white' }}>
                  <SettingsIcon fontSize="large" />
                </Box>
                <Box>
                  <Typography variant="h4" sx={{ fontWeight: 700 }}>
                    {pipelines.filter(p => p.status === 'FAILED' || p.status === 'INVALID').length}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">Issues</Typography>
                </Box>
              </Box>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap', alignItems: 'center' }}>
            <Box sx={{ flexGrow: 1, minWidth: 300 }}>
              <TextField
                placeholder="Search pipelines..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                size="small"
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon color="action" />
                    </InputAdornment>
                  ),
                }}
                sx={{ width: '100%' }}
              />
            </Box>
            <Box sx={{ minWidth: 200 }}>
              <TextField
                select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                size="small"
                sx={{ width: '100%' }}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <FilterListIcon color="action" />
                    </InputAdornment>
                  ),
                }}
              >
                <option value="all">All Statuses</option>
                <option value="DEPLOYED">Deployed</option>
                <option value="DRAFT">Draft</option>
                <option value="VALIDATING">Validating</option>
                <option value="VALID">Valid</option>
                <option value="INVALID">Invalid</option>
                <option value="FAILED">Failed</option>
              </TextField>
            </Box>
          </Box>

          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : filteredPipelines.length === 0 ? (
            <Box sx={{ textAlign: 'center', py: 6 }}>
              <DatabaseIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
              <Typography variant="h6" color="text.secondary" gutterBottom>
                No pipelines found
              </Typography>
              <Typography variant="body1" color="text.secondary" paragraph>
                {search || statusFilter !== 'all' ? 'Try adjusting your filters' : 'Create your first pipeline to get started'}
              </Typography>
              {(!search && statusFilter === 'all') && (
                <Button variant="contained" startIcon={<AddIcon />} component={Link} to="/pipelines/new" sx={{ mt: 2 }}>
                  Create Pipeline
                </Button>
              )}
            </Box>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Pipeline</TableCell>
                    <TableCell>Source → Target</TableCell>
                    <TableCell>Tables</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Throughput</TableCell>
                    <TableCell>Updated</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredPipelines.map((pipeline) => (
                    <TableRow key={pipeline.id} hover>
                      <TableCell>
                        <Box sx={{ display: 'flex', flexDirection: 'column' }}>
                          <Typography variant="body2" sx={{ fontWeight: 600 }}>{pipeline.name}</Typography>
                          <Typography variant="caption" color="text.secondary">{pipeline.description}</Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Chip
                            icon={<DatabaseIcon fontSize="small" />}
                            label={pipeline.sourceType}
                            size="small"
                            variant="outlined"
                            color="primary"
                          />
                          <Typography variant="caption" color="text.secondary">→</Typography>
                          <Chip
                            icon={<SchemaIcon fontSize="small" />}
                            label={pipeline.targetType}
                            size="small"
                            variant="outlined"
                            color="secondary"
                          />
                        </Box>
                      </TableCell>
                      <TableCell>{pipeline.tables}</TableCell>
                      <TableCell>
                        <Chip
                          icon={getStatusIcon(pipeline.status)}
                          label={pipeline.status}
                          size="small"
                          color={getStatusColor(pipeline.status)}
                          variant="outlined"
                        />
                      </TableCell>
                      <TableCell>{pipeline.throughput}</TableCell>
                      <TableCell>
                        {new Date(pipeline.updatedAt).toLocaleDateString('en-US', {
                          year: 'numeric',
                          month: 'short',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </TableCell>
                      <TableCell align="right">
                        <Tooltip title="View Mapping">
                          <IconButton size="small" component={Link} to={`/pipelines/${pipeline.id}/mapping`}>
                            <VisibilityIcon />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Edit Pipeline">
                          <IconButton size="small" component={Link} to={`/pipelines/${pipeline.id}/edit`}>
                            <EditIcon />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Preview Config">
                          <IconButton size="small" component={Link} to={`/pipelines/${pipeline.id}/preview`}>
                            <SettingsIcon />
                          </IconButton>
                        </Tooltip>
                        {pipeline.status !== 'DEPLOYED' && (
                          <Tooltip title="Deploy">
                            <IconButton size="small" component={Link} to={`/pipelines/${pipeline.id}/deploy`}>
                              <CloudUploadIcon />
                            </IconButton>
                          </Tooltip>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}

export default Dashboard;